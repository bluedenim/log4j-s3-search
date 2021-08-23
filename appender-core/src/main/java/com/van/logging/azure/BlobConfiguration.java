package com.van.logging.azure;


/**
 * Configuration for Azure Blob publishing.
 */
public class BlobConfiguration {
    public static final String DEFAULT_LOG_CONTAINER_PATH = "logs/";

    private String storageConnectionString = "";
    private String containerName = "";
    private String blobNamePrefix = DEFAULT_LOG_CONTAINER_PATH;
    private boolean compressionEnabled = false;
    private boolean blobNameGzSuffixEnabled = false;

    public void setStorageConnectionString(String storageConnectionString) {
        this.storageConnectionString = storageConnectionString;
    }

    public void setBlobNameGzSuffixEnabled(boolean gzSuffix) {
        this.blobNameGzSuffixEnabled = gzSuffix;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public void setBlobNamePrefix(String blobNamePrefix) {
        this.blobNamePrefix = blobNamePrefix;
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    public String getStorageConnectionString() {
        return storageConnectionString;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getBlobNamePrefix() {
        return blobNamePrefix;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public boolean isBlobNameGzSuffixEnabled() {
        return blobNameGzSuffixEnabled;
    }

    public String toString() {
        return String.format("Azure blob config (%s:%s, compressed: %s)",
            this.containerName, this.blobNamePrefix, this.compressionEnabled);
    }
}
