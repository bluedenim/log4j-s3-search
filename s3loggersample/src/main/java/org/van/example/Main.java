package org.van.example;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * Example program to exercise the logger.  Together with the log4j.properties
 * in this project, this program will demonstrate logging batches into S3.
 * <br>
 * The program will basically loop for a period of time while logging messages in various
 * levels at small intervals to simulate typical logging patterns in a long-lived process.
 *
 * @author vly
 *
 */
public class Main {

	private static Logger logger =
		Logger.getLogger(Main.class);
	
	public static void main(String[] args) throws InterruptedException {
		
		logger.info("Hello from Main!");
		Long started = System.currentTimeMillis();
		Long now = System.currentTimeMillis();
        // Loop for 3 minutes
		while (now - started < TimeUnit.MINUTES.toMillis(3)) {
			logger.info("Another round through the loop!");
			logger.warn("This is a warning!");
			logger.error("And this is an error!!!");
            // Sleep for 7 seconds before logging messages again so we don't produce too much data
			Thread.sleep(TimeUnit.SECONDS.toMillis(7));
		}
	}

}
