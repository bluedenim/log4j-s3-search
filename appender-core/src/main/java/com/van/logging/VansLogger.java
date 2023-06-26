package com.van.logging;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Logger used by the log4j-s3-search classes for internal status and errors.
 */
public class VansLogger {
    public static final Logger logger = LogManager.getLogger(VansLogger.class);
}
