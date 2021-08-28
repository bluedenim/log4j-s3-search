package com.van.logging.azure;

import com.van.logging.PublishContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class BlobPublishHelperTest {

    private PublishContext context;
    private BlobConfiguration blobConfiguration;

    @Before
    public void setUp() {
        context = new PublishContext("ghi", "localhost", new String[]{});
        blobConfiguration = new BlobConfiguration();
    }

    @Test
    public void testGetBlobNameWithoutSuffix() {
        blobConfiguration.setCompressionEnabled(true);
        blobConfiguration.setBlobNameGzSuffixEnabled(false);
        String blobName = BlobPublishHelper.getBlobName(context, "abcdef/", blobConfiguration);

        Assert.assertEquals(blobName, "abcdef/ghi");
    }

    @Test
    public void testGetBlobNameWithSuffix() {
        blobConfiguration.setCompressionEnabled(true);
        blobConfiguration.setBlobNameGzSuffixEnabled(true);
        String blobName = BlobPublishHelper.getBlobName(context, "abcdef/", blobConfiguration);

        Assert.assertEquals(blobName, "abcdef/ghi.gz");
    }

    @Test
    public void testGetBlobNameWithoutCompression() {
        blobConfiguration.setCompressionEnabled(false);
        blobConfiguration.setBlobNameGzSuffixEnabled(true);
        String blobName = BlobPublishHelper.getBlobName(context, "abcdef/", blobConfiguration);

        Assert.assertEquals(blobName, "abcdef/ghi");
    }
}
