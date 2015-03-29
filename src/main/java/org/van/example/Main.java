package org.van.example;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * Example program to exercise the logger.  Together with the log4j.properties
 * in this project, this program will demonstrate logging batches into S3.
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
		while (now - started < TimeUnit.MINUTES.toMillis(3)) {
			logger.info("Another round through the loop!");
			logger.warn("This is a warning!");
			logger.error("And this is an error!!!");
			Thread.sleep(TimeUnit.SECONDS.toMillis(7));
		}
	}

}
