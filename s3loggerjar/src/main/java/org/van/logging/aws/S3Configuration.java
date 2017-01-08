package org.van.logging.aws;

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
    
    private String accessKey = null;
    private String secretKey = null;
    private Region region = null;
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

    public Region getRegion() {
        return region;
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
