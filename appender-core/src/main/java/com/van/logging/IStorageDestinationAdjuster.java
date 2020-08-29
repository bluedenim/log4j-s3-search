package com.van.logging;


/**
 * Interface for adjustment logic to tweak the destination of a cloud storage upload operation done for
 * each batch of events.
 */
public interface IStorageDestinationAdjuster {

    /**
     * Given the path that is about to be used, the implementation may replace it with a totally
     * different path (perhaps based on the path provided).
     *
     * @param path the path that is about to be used.
     *
     * @return a new path to be used (or null if the provided path is acceptable)
     */
    public String adjustPath(String path);
}
