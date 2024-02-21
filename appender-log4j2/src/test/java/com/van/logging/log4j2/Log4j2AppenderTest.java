package com.van.logging.log4j2;

import com.van.logging.LoggingEventCache;
import junit.framework.TestCase;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;

import static org.apache.logging.log4j.core.AbstractLifeCycle.DEFAULT_STOP_TIMEUNIT;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class Log4j2AppenderTest extends TestCase {

    public void testClose() {
        final String name = "test";
        final Filter filter = mock(Filter.class);
        final Layout layout = mock(Layout.class);
        final LoggingEventCache loggingEventCache = mock(LoggingEventCache.class);
        final Log4j2Appender appender = new Log4j2Appender(name, filter, layout, false, loggingEventCache);

        expect(loggingEventCache.flushAndPublish(true)).andReturn(null);
        expect(loggingEventCache.stop()).andReturn(true);
        replay(loggingEventCache);
        appender.stop(0, DEFAULT_STOP_TIMEUNIT);
        verify(loggingEventCache);
    }
}
