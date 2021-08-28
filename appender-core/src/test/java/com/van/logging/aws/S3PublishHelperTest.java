package com.van.logging.aws;

import com.van.logging.PublishContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class S3PublishHelperTest {

    private PublishContext context;

    @Before
    public void setUp() {
        context = new PublishContext("ghi", "localhost", new String[]{});
    }

    @Test
    public void testGetKeyWithoutSuffix() {
        String key = S3PublishHelper.getKey(context, "abcdef/", true, false);

        Assert.assertEquals(key, "abcdef/ghi");
    }

    @Test
    public void testGetKeyWithSuffix() {
        String key = S3PublishHelper.getKey(context, "abcdef/", true, true);

        Assert.assertEquals(key, "abcdef/ghi.gz");
    }

    @Test
    public void testGetKeyWithoutCompression() {
        String key = S3PublishHelper.getKey(context, "abcdef/", false, true);

        Assert.assertEquals(key, "abcdef/ghi");
    }
}
