package com.van.logging.utils;

import com.van.logging.IStorageDestinationAdjuster;


/**
 * Helper utils for publishing.
 */
public class PublishHelperUtils {

    /**
     * Adjust the path for storage upload if necessary.
     *
     * @param originalPath the original path to use.
     * @param adjuster an optional adjuster that will provide a modification/replacement value for the path
     *
     * @return the adjusted path (may be equal to originalPath).
     *
     * Note that if the adjuster returned null, then the original path is returned
     *
     * Lastly, however the result is achieved (either from the adjuster or the original path), if the value is null,
     * then "" will be returned.
     */
    public static String adjustStoragePathIfNecessary(String originalPath, IStorageDestinationAdjuster adjuster) {
        String path = originalPath;
        if (null != adjuster) {
            String newPath = adjuster.adjustPath(path);
            if (null != newPath) {
                path = newPath;
            }
        }
        if (null == path) {
            path = "";
        }
        return path;
    }
}
