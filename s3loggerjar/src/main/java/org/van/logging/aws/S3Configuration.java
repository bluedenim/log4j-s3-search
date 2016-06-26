package org.van.logging.aws;

import com.amazonaws.regions.Regions;

/**
 * S3 connectivity/configuration 
 * 
 * @author vly
 *
 */
public class S3Configuration {
	public static final String DEFAULT_AWS_REGION = Regions.US_WEST_2.name();
	public static final String DEFAULT_LOG_BUCKETPATH = "logs/";
	
	private String accessKey = null;
	private String secretKey = null;
	private String region = DEFAULT_AWS_REGION;
	private String bucket = null;
	private String path = DEFAULT_LOG_BUCKETPATH;
	
	public String getAccessKey() {
		return accessKey;
	}
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}
	public String getSecretKey() {
		return secretKey;
	}
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	public String getRegion() {
		return region;
	}
	public void setRegion(String region) {
		this.region = region;
	}
	public String getBucket() {
		return bucket;
	}
	public void setBucket(String bucket) {
		this.bucket = bucket;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

}
