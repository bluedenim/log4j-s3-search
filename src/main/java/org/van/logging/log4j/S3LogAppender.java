package org.van.logging.log4j;

import java.net.URL;
import java.util.UUID;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;
import org.van.logging.LoggingEventCache;
import org.van.logging.aws.AwsClientBuilder;
import org.van.logging.aws.S3Configuration;
import org.van.logging.aws.S3PublishHelper;
import org.van.logging.solr.SolrConfiguration;
import org.van.logging.solr.SolrPublishHelper;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * The log appender adapter that hooks into the Log4j framework to collect
 * logging events.
 * <br>
 * <h2>General</h2>
 * In addition to the typical log appender parameters, this appender also
 * supports (some are required) these parameters:
 * <br>
 * <ul>
 *   <li>stagingBufferSize -- the buffer size to collect log events before 
 *   		publishing them in a batch (e.g. 20000).(</li>
 *   <li>tags -- comma delimited list of additional tags to associate with the
 *   		events (e.g. "MainSite;Production").</li>
 * </ul>
 * <br>
 * <h2>S3</h2>
 * These parameters configure the S3 publisher:
 * <br>
 * <ul>
 * 	 <li>s3AccessKey -- (optional) the access key component of the AWS 
 *     credentials</li>
 *   <li>s3SecretKey -- (optional) the secret key component of the AWS
 *     credentials</li>
 *   <li>s3Bucket -- the bucket name in S3 to use</li>
 *   <li>s3Path -- the path (key prefix) to use to compose the final key
 *     to use to store the log events batch</li>
 * </ul>
 * <em>NOTES</em>:
 * <ul>
 *   <li>If the access key and secret key are provided, they will be preferred 
 *   	over whatever default setting (e.g. ~/.aws/credentials or 
 * 		%USERPROFILE%\.aws\credentials) is in place for the 
 * 		runtime environment.</li>
 *   <li>Tags are currently ignored by the S3 publisher.</li>
 * </ul>
 * <br>
 * <h2>Solr</h2>
 * These parameters configure the Solr publisher:
 * <br>
 * <ul>
 *   <li>solrUrl -- the URL to where your Solr core/collection is
 *     (e.g. "http://localhost:8983/solr/mylogs/")</li>
 * </ul>
 * @author vly
 *
 */
public class S3LogAppender extends AppenderSkeleton
	implements Appender, OptionHandler {
	
	static final int DEFAULT_THRESHOLD = 2000;
	static final int MONITOR_PERIOD = 30;

	private int stagingBufferSize = DEFAULT_THRESHOLD;	
	
	private LoggingEventCache stagingLog = null;
	
	private volatile String[] tags;
	private volatile String hostName;
	
	private S3Configuration s3;
	private SolrConfiguration solr;
	private AmazonS3Client s3Client;	
	
	@Override
	public void close() {
		System.out.println("close(): Cleaning up resources");
		if (null != stagingLog) {
			stagingLog.flushAndPublishQueue(true);
			stagingLog = null;
		}
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

	public void setStagingBufferSize(int buffer) {
		stagingBufferSize = buffer;
	}	
	
	
	// S3 properties
	///////////////////////////////////////////////////////////////////////////	
	public S3Configuration getS3() {
		if (null == s3) {
			s3 = new S3Configuration();
		}
		return s3;
	}
	
	public void setS3Bucket(String bucket) {
		getS3().setBucket(bucket);
	}
	
	public void setS3Path(String path) {
		getS3().setPath(path);
	}
	
	public void setS3AwsKey(String accessKey) {
		getS3().setAccessKey(accessKey);
	}
	
	public void setS3AwsSecret(String secretKey) {
		getS3().setSecretKey(secretKey);
	}
	
	// Solr properties
	///////////////////////////////////////////////////////////////////////////
	public void setSolrUrl(String url) {
		if (null == solr) {
			solr = new SolrConfiguration();
		}
		solr.setUrl(url);
	}
	
	public void setTags(String tags) {
		if (null != tags) {
			this.tags = tags.split("[,;]");
			for (int i = 0; i < this.tags.length; i++) {
				this.tags[i] = this.tags[i].trim();
			}
		}		
	}

	@Override
	protected void append(LoggingEvent evt) {
		try {
			stagingLog.add(evt);
		} catch (Exception ex) {	
			errorHandler.error("Cannot append event", ex, 105, evt);
		}		
	}
	
	@Override
	public void activateOptions() {
		super.activateOptions();
		try {
			initFilters();
			java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
			hostName = addr.getHostName();
			if (null != s3) {
				AwsClientBuilder builder = 
					new AwsClientBuilder(Regions.valueOf(s3.getRegion()),
						s3.getAccessKey(), s3.getSecretKey());
				s3Client = builder.build(AmazonS3Client.class);
			}
			initStagingLog();
		} catch (Exception ex) {
			errorHandler.error("Cannot initialize resources", ex, 100);
		}
	}	

	void initFilters() {
		addFilter(new Filter() {
			@Override
			public int decide(LoggingEvent event) {
				// To prevent infinite looping, we filter out events from
				// the publishing thread
				int decision = Filter.NEUTRAL;
				if (LoggingEventCache.PUBLISH_THREAD_NAME.equals(event.getThreadName())) {
					decision = Filter.DENY;
				}
				return decision;
		}});
	}
	
	void initStagingLog() throws Exception {
		if (null == stagingLog) {
			CachePublisher publisher = new CachePublisher(layout, hostName, tags);
			if (null != s3Client) {
				publisher.addHelper(new S3PublishHelper(s3Client,
					s3.getBucket(), s3.getPath()));
			}
			if (null != solr) {
			URL solrUrl = solr.getUrl();
				if (null != solrUrl) {
					publisher.addHelper(new SolrPublishHelper(solrUrl));
				}
			}
			UUID uuid = UUID.randomUUID();
			stagingLog = new LoggingEventCache(
				uuid.toString().replaceAll("-",""), stagingBufferSize, 
				publisher);
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.out.println("Publishing staging log on shutdown...");
					stagingLog.flushAndPublishQueue(true);
				}
			});
		}
	}
	
}
