package com.van.logging.utils;

/**
 * Useful String utilities added here to avoid having to add a dependency to other packages.
 */
public class StringUtils {

    /**
     * Checks to see if a String is defined and not empty after trimming
     *
     * @param s the String to test
     *
     * @return true if the string is not null and is not empty
     */
    public static boolean isTruthy(String s) {
        return (null != s && !s.trim().isEmpty());
    }

}
