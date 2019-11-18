package com.van.logging.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

/**
 * S3 connectivity/configuration
 *
 * @author vly
 *
 */
public class S3Configuration {
    public static final String DEFAULT_LOG_BUCKETPATH = "logs/";

    /**
     * SSE options when using S3
     * See https://docs.aws.amazon.com/AmazonS3/latest/dev/serv-side-encryption.html
     */
    public enum SSEType {
        SSE_S3,     // Server-Side Encryption with Amazon S3-Managed Key
        SSE_KMS,    // Server-Side Encryption with AWS KMS-Managed Keys
        SSE_C,      // Server-Side Encryption with Customer-Provided Keys
    }

    /**
     * Configuration for working with SSE in AWS S3
     */
    public final static class S3SSEConfiguration {
        private final SSEType keyType;
        private final String keyId;

        public S3SSEConfiguration(SSEType keyType, String keyId) {
            if (keyType != SSEType.SSE_S3) {
                throw new UnsupportedOperationException("Only SSE_S3 is supported at this time.");
            }
            this.keyType = keyType;
            this.keyId = keyId;
        }

        public SSEType getKeyType() {
            return keyType;
        }

        public String getKeyId() {
            return keyId;
        }
    }

    private String accessKey = null;
    private String secretKey = null;
    private String sessionToken = null;
    private Region region = null;
    private String bucket = null;
    private String path = DEFAULT_LOG_BUCKETPATH;

    private String serviceEndpoint = null;
    private String signingRegion = null;

    private S3SSEConfiguration sseConfiguration = null;

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
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public Region getRegion() {
        return region;
    }

    public S3SSEConfiguration getSseConfiguration() {
        return sseConfiguration;
    }

    public void setSseConfiguration(S3SSEConfiguration sseConfiguration) {
        this.sseConfiguration = sseConfiguration;
    }

    /**
     * Sets the region to use when building the S3 client. The value can be the
     * "lowercase dash" format used in AWS literature (e.g. "us-west-2", "eu-west-1")
     * or the Regions ordinal name (e.g. "US_WEST_2" or "EU_WEST_2").
     * <br>
     * If the value is <code>null</code>, then no region will be explicitly set.
     *
     * @param regionName the region name (e.g. "us-west-2", "eu-west-1")
     */
    public void setRegion(String regionName) {
        this.region = resolveRegion(regionName);
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

    public String getServiceEndpoint() {
        return serviceEndpoint;
    }
    public void setServiceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }
    public String getSigningRegion() {
        return signingRegion;
    }
    public void setSigningRegion(String signingRegion) {
        this.signingRegion = signingRegion;
    }

    /**
     * Best-effort to map a region name to an actual Region instance. The input
     * string can be either the public region name (e.g. "us-west-1") or the
     * Regions enum ordinal name (e.g. "US_WEST_1").
     *
     * @param str the region name to map to a Region
     *
     * @return the mapped Region
     *
     * @throws IllegalArgumentException if the input name cannot be mapped to a
     *  Region.
     */
    static Region resolveRegion(String str) {
        Region region = null;
        if ((null != str) && (str.length() > 0)) {
            Regions regions;
            try {
                regions = Regions.valueOf(str);
            } catch (IllegalArgumentException ex) {
                // Try to interpret as the public name (this is more expensive). If
                // this fails, then just propagate the IllegalArgumentException out.
                regions = Regions.fromName(str);
            }
            region = Region.getRegion(regions);
        }
        return region;
    }
}
