package com.van.logging.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertEquals;
import static org.easymock.EasyMock.anyObject;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AwsClientHelpers.class, AmazonS3ClientBuilder.class})
public class AwsClientHelpersTest {

    String accessKey;
    String secretKey;
    Region region;
    String serviceEndpoint;
    String signingRegion;

    @Before
    public void setUp() {
        accessKey = "BLAHBLAHBLAH";
        secretKey = "MOREBLAHBLAHBLAHBLAH";
        region = Region.getRegion(Regions.US_WEST_1);
        serviceEndpoint = "blahblahblah.blahblahblah.com";
        signingRegion = "us-west-1";
    }

    @Test
    public void testBuildClientWithRegion() {
        /**
         *  Tests that the region is set on the client builder before the client is built.
         */
        AmazonS3 mockedClient = PowerMock.createMock(AmazonS3.class);
        AmazonS3ClientBuilder mockedS3ClientBuilder = PowerMock.createMock(AmazonS3ClientBuilder.class);
        PowerMock.mockStaticPartial(AwsClientHelpers.class, "getS3ClientBuilder");
        EasyMock.expect(AwsClientHelpers.getS3ClientBuilder()).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.withCredentials(anyObject())).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.withPathStyleAccessEnabled(false)).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.withRegion(region.getName())).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.build()).andReturn(mockedClient);
        PowerMock.replayAll();

        try {
            AmazonS3 client = AwsClientHelpers.buildClient(
                accessKey, secretKey, null, region,
                null, null,
                false
            );
            assertEquals(mockedClient, client);

            // Not verifying AwsClientHelpers.class due to some odd behavior not related to the test.
            // Maybe related to https://stackoverflow.com/questions/21690492/using-easymock-with-recursive-method
            // but not gonna spend time on this right now since it's not relevant to the test.
            PowerMock.verify(mockedClient, mockedS3ClientBuilder);

        } catch (Exception ex) {
            fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test
    public void testBuildClientWithServiceEndpoint() {
        /**
         *  Tests that the service endpoint and signing region are set on the client builder before the client is built.
         */
        AmazonS3 mockedClient = PowerMock.createMock(AmazonS3.class);
        AmazonS3ClientBuilder mockedS3ClientBuilder = PowerMock.createMock(AmazonS3ClientBuilder.class);
        PowerMock.mockStaticPartial(AwsClientHelpers.class, "getS3ClientBuilder");
        EasyMock.expect(AwsClientHelpers.getS3ClientBuilder()).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.withCredentials(anyObject())).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.withPathStyleAccessEnabled(false)).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.withEndpointConfiguration(
            anyObject(AwsClientBuilder.EndpointConfiguration.class
        ))).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.build()).andReturn(mockedClient);
        PowerMock.replayAll();

        try {
            AmazonS3 client = AwsClientHelpers.buildClient(
                accessKey, secretKey, null, null,
                serviceEndpoint, signingRegion,
                false
            );
            assertEquals(mockedClient, client);

            // Not verifying AwsClientHelpers.class due to some odd behavior not related to the test.
            // Maybe related to https://stackoverflow.com/questions/21690492/using-easymock-with-recursive-method
            // but not gonna spend time on this right now since it's not relevant to the test.
            PowerMock.verify(mockedClient, mockedS3ClientBuilder);

        } catch (Exception ex) {
            fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test
    public void testBuildClientWithPathStyleAccess() {
        /**
         *  Tests that the pathStyleAccess option is set on the client builder before the client is built.
         */
        AmazonS3 mockedClient = PowerMock.createMock(AmazonS3.class);
        AmazonS3ClientBuilder mockedS3ClientBuilder = PowerMock.createMock(AmazonS3ClientBuilder.class);
        PowerMock.mockStaticPartial(AwsClientHelpers.class, "getS3ClientBuilder");
        EasyMock.expect(AwsClientHelpers.getS3ClientBuilder()).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.withCredentials(anyObject())).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.withPathStyleAccessEnabled(true)).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.withEndpointConfiguration(
            anyObject(AwsClientBuilder.EndpointConfiguration.class
            ))).andReturn(mockedS3ClientBuilder);
        EasyMock.expect(mockedS3ClientBuilder.build()).andReturn(mockedClient);
        PowerMock.replayAll();

        try {
            AmazonS3 client = AwsClientHelpers.buildClient(
                accessKey, secretKey, null, null,
                serviceEndpoint, signingRegion,
                true
            );
            assertEquals(mockedClient, client);

            // Not verifying AwsClientHelpers.class due to some odd behavior not related to the test.
            // Maybe related to https://stackoverflow.com/questions/21690492/using-easymock-with-recursive-method
            // but not gonna spend time on this right now since it's not relevant to the test.
            PowerMock.verify(mockedClient, mockedS3ClientBuilder);

        } catch (Exception ex) {
            fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test
    public void testGetCredentialsProvider() {
        AWSCredentialsProvider accessProvider = AwsClientHelpers.getCredentialsProvider(
            "accessKey", "secretKey", null
        );
        AWSCredentials credentials = accessProvider.getCredentials();
        assertEquals(BasicAWSCredentials.class, credentials.getClass());
    }

    @Test
    public void testGetCredentialsProviderWithSessionToken() {
        AWSCredentialsProvider accessProvider = AwsClientHelpers.getCredentialsProvider(
            "accessKey", "secretKey", "sessionToken"
        );
        AWSCredentials credentials = accessProvider.getCredentials();
        assertEquals(BasicSessionCredentials.class, credentials.getClass());
        BasicSessionCredentials bsc = (BasicSessionCredentials)credentials;
        assertEquals("sessionToken", bsc.getSessionToken());
    }
}
