package com.van.logging.log4j;

import com.van.logging.IStorageDestinationAdjuster;
import com.van.logging.utils.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;


/**
 * A path adjuster that assumes the original path is a template to expand using a limited-scoped implementation of
 * PatternLayout.
 * Currently, only the date (i.e. %d{...}) template is supported.
 */
public class PatternedPathAdjuster implements IStorageDestinationAdjuster {

    @Override
    public String adjustPath(String path) {
        String adjusted = path;
        if (StringUtils.isTruthy(path)) {
            // e.g. "%d{yyyy-MM-dd}"
            PatternLayout layout = new PatternLayout(path);
            LoggingEvent evt = new LoggingEvent(
                null,
                null,
                System.currentTimeMillis(),
                Level.INFO, null,
                Thread.currentThread().getName(),
                null,
                null,
                null,
                null
            );
            adjusted = layout.format(evt);
        }
        return adjusted;
    }
}
