package com.van.logging.log4j2;

import com.van.logging.IStorageDestinationAdjuster;
import com.van.logging.utils.StringUtils;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;


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
            StringBuilder sb = new StringBuilder();
            PatternLayout layout = PatternLayout.newBuilder().withPattern(path).build();
            MutableLogEvent evt = new MutableLogEvent();
            evt.setTimeMillis(System.currentTimeMillis());
            layout.serialize(evt, sb);
            adjusted = sb.toString();
        }
        return adjusted;
    }
}
