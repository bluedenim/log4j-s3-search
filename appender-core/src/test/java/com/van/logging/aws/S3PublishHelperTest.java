package com.van.logging.aws;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class S3PublishHelperTest {

    @Test
    public void testCreateCompressedStreamAsNecessary() {
        try (OutputStream os = new ByteArrayOutputStream()) {
            try (OutputStream testStream =
                     S3PublishHelper.createCompressedStreamAsNecessary(os, false)) {
                Assert.assertEquals(ByteArrayOutputStream.class, testStream.getClass());
            }

            try (OutputStream testStream =
                     S3PublishHelper.createCompressedStreamAsNecessary(os, true)) {
                Assert.assertEquals(GZIPOutputStream.class, testStream.getClass());
            }
        } catch (IOException e) {
            Assert.fail(
                String.format("Fail due to exception: %s", e.getMessage()));
        }
    }
}
