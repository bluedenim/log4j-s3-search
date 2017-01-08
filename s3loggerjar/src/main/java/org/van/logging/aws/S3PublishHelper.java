package org.van.logging.aws;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.apache.http.entity.ContentType;
import org.apache.log4j.spi.LoggingEvent;
import org.van.logging.PublishContext;
import org.van.logging.log4j.IPublishHelper;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * Implementation to publish log events to S3.
 * <br>
 * These Log4j logger parameters configure the S3 publisher:
 * <br>
 * <em>NOTES</em>:
 * <ul>
 * <li>If the access key and secret key are provided, they will be preferred over
 * whatever default setting (e.g. ~/.aws/credentials or 
 * %USERPROFILE%\.aws\credentials) is in place for the 
 * runtime environment.</li>
 * <li>Tags are currently ignored by the S3 publisher.</li>
 * </ul>
 * 
 * @author vly
 *
 */
public class S3PublishHelper implements IPublishHelper {
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	private static final String S3ERRCODE_BUCKETALREADYOWNEDBYYOU = "BucketAlreadyOwnedByYou";
	
	private final AmazonS3Client client;
	private final String bucket;
	private final String path;
	
	private volatile boolean bucketExists = false;
	private volatile StringBuilder stringBuilder;
	
	public S3PublishHelper(AmazonS3Client client, String bucket, String path) {
		this.client = client;
		this.bucket = bucket.toLowerCase();
		if (!path.endsWith("/")) {
			this.path = path + "/";
		} else {
			this.path = path;
		}
	}
	
	public void publish(PublishContext context, int sequence, LoggingEvent event) {
		stringBuilder.append(context.getLayout().format(event))
			.append(LINE_SEPARATOR);
	}

	public void start(PublishContext context) {
		stringBuilder = new StringBuilder();
		
		// There are two ways to go about this: either I call something like
		// getBucketLocation()/listBuckets() and check to see if the bucket
		// is there.  If not, I need to create the bucket.  This requires 2
		// requests max to S3.
		// Or I go ahead and try to create the bucket.  If it's there already,
		// I get an exception.  If it's not, then (hopefully) it gets created.
		// In either case, we only incur 1 hit to S3.
		// A third option is to just assume the bucket is created a priori.
		
		// For now, I've chosen the 2nd option.
		if (!bucketExists) {
			try {
				client.createBucket(bucket);
				bucketExists = true;
			} catch (AmazonS3Exception ex) {
				if (S3ERRCODE_BUCKETALREADYOWNEDBYYOU.equals(ex.getErrorCode())) {
					// If the exception is due to the bucket already existing,
					// then swallow it.  This is "normal."
					bucketExists = true;
				} else {
					throw ex;
				}
			}
		}
	}

	public void end(PublishContext context) {
		String key = String.format("%s%s", path, context.getCacheName());
		/* System.out.println(String.format("Publishing to S3 (bucket=%s; key=%s):",
			bucket, key)); */
		
		String data = stringBuilder.toString();
		/* System.out.println(data); */
		try {
			ObjectMetadata metadata = new ObjectMetadata();
			byte bytes[] = data.getBytes("UTF-8");
			metadata.setContentLength(bytes.length);
			metadata.setContentType(ContentType.TEXT_PLAIN.getMimeType());
			ByteArrayInputStream is = new ByteArrayInputStream(bytes);
			PutObjectResult result = client.putObject(bucket, 
				key, is, metadata);
			/* System.out.println(String.format("Content MD5: %s",
				result.getContentMd5())); */
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		stringBuilder = null;
	}

}
