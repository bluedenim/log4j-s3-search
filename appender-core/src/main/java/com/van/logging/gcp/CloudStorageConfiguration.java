package com.van.logging.gcp;

/**
 * Configuration for Google Cloud Storage logs.
 *
 * As far as authentication, the default mechanism will be used until custom support is needed.
 * See https://github.com/googleapis/google-cloud-java#authentication for instructions on setting
 * thins up.
 *
 * The simplest way is to download the JSON somewhere and set the environment variable
 * GOOGLE_APPLICATION_CREDENTIALS to point to it.
 *
 */
public class CloudStorageConfiguration {
    public static final String DEFAULT_BLOB_PATH = "logs/";

    private String bucketName;
    private String blobNamePrefix = DEFAULT_BLOB_PATH;
    private boolean compressionEnabled = false;
    private boolean blobNameGzSuffixEnabled = false;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBlobNamePrefix() {
        return blobNamePrefix;
    }

    public void setBlobNamePrefix(String blobNamePrefix) {
        this.blobNamePrefix = blobNamePrefix;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    public boolean isBlobNameGzSuffixEnabled() {
        return blobNameGzSuffixEnabled;
    }

    public void setBlobNameGzSuffixEnabled(boolean blobNameGzSuffixEnabled) {
        this.blobNameGzSuffixEnabled = blobNameGzSuffixEnabled;
    }
}
