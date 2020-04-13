package com.van.logging;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class AbstractFilePublishHelperTest {

    @Test
    public void testCreateCompressedStreamAsNecessary() {
        try (OutputStream os = new ByteArrayOutputStream()) {
            try (OutputStream testStream = AbstractFilePublishHelper.createCompressedStreamAsNecessary(os, false, false)) {
                Assert.assertEquals(ByteArrayOutputStream.class, testStream.getClass());
            }

            try (OutputStream testStream = AbstractFilePublishHelper.createCompressedStreamAsNecessary(os, true, false)) {
                Assert.assertEquals(GZIPOutputStream.class, testStream.getClass());
            }
        } catch (IOException e) {
            Assert.fail(
                    String.format("Fail due to exception: %s", e.getMessage()));
        }
    }
}
