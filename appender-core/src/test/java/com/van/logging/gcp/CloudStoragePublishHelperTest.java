package com.van.logging.gcp;

import com.van.logging.PublishContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class CloudStoragePublishHelperTest {

    private PublishContext context;
    private CloudStorageConfiguration configuration;

    @Before
    public void setUp() {
        context = new PublishContext("ghi", "localhost", new String[]{});
        configuration = new CloudStorageConfiguration();
    }

    @Test
    public void testGetBlobNameWithoutSuffix() {
        configuration.setCompressionEnabled(true);
        configuration.setBlobNameGzSuffixEnabled(false);
        String blobName = CloudStoragePublishHelper.getBlobName(context, "abcdef/", configuration);

        Assert.assertEquals(blobName, "abcdef/ghi");
    }

    @Test
    public void testGetBlobNameWithSuffix() {
        configuration.setCompressionEnabled(true);
        configuration.setBlobNameGzSuffixEnabled(true);
        String blobName = CloudStoragePublishHelper.getBlobName(context, "abcdef/", configuration);

        Assert.assertEquals(blobName, "abcdef/ghi.gz");
    }

    @Test
    public void testGetBlobNameWithoutCompression() {
        configuration.setCompressionEnabled(false);
        configuration.setBlobNameGzSuffixEnabled(true);
        String blobName = CloudStoragePublishHelper.getBlobName(context, "abcdef/", configuration);

        Assert.assertEquals(blobName, "abcdef/ghi");
    }
}
