package com.van.logging.utils;

import com.van.logging.IStorageDestinationAdjuster;


/**
 * Helper utils for publishing.
 */
public class PublishHelperUtils {

    /**
     * Adjust the path for storage upload if necessary. If the adjusted path is null, then "" will be used.
     *
     * @param originalPath the original path to use
     * @param adjuster an optional adjuster that will provide a modification/replacement value for the path
     *
     * @return the adjusted path (may be equal to originalPath)
     */
    public static String adjustStoragePathIfNecessary(String originalPath, IStorageDestinationAdjuster adjuster) {
        String path = originalPath;
        if (null != adjuster) {
            String newPath = adjuster.adjustPath(path);
            if (StringUtils.isTruthy(newPath)) {
                path = newPath;
            }
        }
        if (StringUtils.isTruthy(path)) {
            if (!path.endsWith("/")) {
                path = path + "/";
            }
        } else {
            path = "";
        }
        return path;
    }
}
