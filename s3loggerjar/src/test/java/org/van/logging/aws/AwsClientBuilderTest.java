package org.van.logging.aws;


import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest({AwsClientBuilder.class})
public class AwsClientBuilderTest {

    @Test
    public void testBuildWithRegion() {
        Region region = Region.getRegion(Regions.US_WEST_1);
        AmazonWebServiceClient mockedS3 = createMock(AmazonWebServiceClient.class);
        AwsClientBuilder builder = createPartialMock(AwsClientBuilder.class,
            new String[] {"instantiateClient"}, region);
        try {
            expect(builder.instantiateClient(AmazonS3Client.class))
                .andReturn(mockedS3);
            mockedS3.setRegion(region);
            expectLastCall();
            replay(mockedS3, builder);

            builder.build(AmazonS3Client.class);

            verify(mockedS3, builder);
        } catch (Exception ex) {
            fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test
    public void testBuildWithoutRegion() {
        AmazonWebServiceClient mockedS3 = createMock(AmazonWebServiceClient.class);
        try {
            AwsClientBuilder builder = createPartialMockAndInvokeDefaultConstructor(AwsClientBuilder.class,
                "instantiateClient");
            expect(builder.instantiateClient(AmazonS3Client.class))
                .andReturn(mockedS3);
            replay(mockedS3, builder);

            builder.build(AmazonS3Client.class);

            verify(mockedS3, builder);
        } catch (Exception ex) {
            fail("Unexpected exception: " + ex.getMessage());
        }
    }
}
