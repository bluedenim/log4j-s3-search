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

    /**
     * If the string passed in is not blank or null and did not end with the trailing value, then the trailing value
     * will be appended to it in the returned value.
     *
     * @param s the string to conditionally add the trailing value to. If null, then null will be returned.
     * @param trailing the trailing value to append. If null, then s will be returned unmodified.
     *
     * @return the string s with the trailing value appended as necessary.
     */
    public static String addTrailingIfNeeded(String s, String trailing) {
        String value = s;
        if ((null != s) && (null != trailing)) {
            if (isTruthy(s) && !s.endsWith(trailing)) {
                value = s + trailing;
            }
        }
        return value;
    }
}
