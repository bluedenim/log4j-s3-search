package com.van.logging.utils;

import org.junit.Assert;
import org.junit.Test;

public class PublishHelperUtilsTest {

    /**
     * Missing adjuster should just return the original value, and if it's null, then return ""
     */
    @Test
    public void testAdjustStoragePathIfNecessaryNull() {
        Assert.assertEquals(
            "",
            PublishHelperUtils.adjustStoragePathIfNecessary(null, null)
        );
    }

    /**
     * Missing adjuster should just return the original value (as long as it's not null)
     */
    @Test
    public void testAdjustStoragePathIfNecessaryNullAdjuster() {
        Assert.assertEquals(
            "Hello",
            PublishHelperUtils.adjustStoragePathIfNecessary("Hello", null)
        );
    }

    @Test
    public void testAdjustStoragePathIfNecessaryUseAdjusterResponse() {
        final String response = "Hello";
        Assert.assertEquals(
            response,
            PublishHelperUtils.adjustStoragePathIfNecessary(
                "blah",
                (String path) -> { return response; }
            )
        );
    }

    @Test
    public void testAdjustStoragePathIfNecessaryAdjusterNullResponse() {
        String originalPath = "Hello";
        Assert.assertEquals(
            originalPath,
            PublishHelperUtils.adjustStoragePathIfNecessary(
                originalPath,
                (String path) -> { return null; }
            )
        );
    }
}
