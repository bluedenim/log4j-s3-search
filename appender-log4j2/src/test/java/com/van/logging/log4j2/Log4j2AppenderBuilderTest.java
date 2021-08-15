package com.van.logging.log4j2;

import com.van.logging.aws.S3Configuration;
import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class Log4j2AppenderBuilderTest extends TestCase {

    Log4j2AppenderBuilder initializeBuilderWithCompression(boolean compressionEnabled)
    throws NoSuchFieldException, IllegalAccessException {
        Log4j2AppenderBuilder builder = new Log4j2AppenderBuilder();
        Class<? extends Log4j2AppenderBuilder> clz = builder.getClass();

        Field field = clz.getDeclaredField("s3Compression");
        field.setAccessible(true);
        field.set(builder, String.valueOf(compressionEnabled));

        // S3Configuration requires s3Bucket to be set
        field = clz.getDeclaredField("s3Bucket");
        field.setAccessible(true);
        field.set(builder, "testBucket");

        return builder;
    }

    public void testS3CompressionSetting() {
        Log4j2AppenderBuilder builder = null;
        try {
            builder = initializeBuilderWithCompression(true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Cannot set the fields in Log4j2AppenderBuilder for the test");
        }
        assertNotNull(builder);
        Optional<S3Configuration> maybeS3Config = builder.getS3ConfigIfEnabled();
        assertTrue(maybeS3Config.isPresent());

        maybeS3Config.ifPresent((s3Configuration -> {
            assertTrue(s3Configuration.isCompressionEnabled());
        }));
    }

    public void testS3CompressionSettingDisabled() {
        Log4j2AppenderBuilder builder = null;
        try {
            builder = initializeBuilderWithCompression(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Cannot set the fields in Log4j2AppenderBuilder for the test");
        }
        assertNotNull(builder);
        Optional<S3Configuration> maybeS3Config = builder.getS3ConfigIfEnabled();
        assertTrue(maybeS3Config.isPresent());

        maybeS3Config.ifPresent((s3Configuration -> {
            assertFalse(s3Configuration.isCompressionEnabled());
        }));
    }
}
