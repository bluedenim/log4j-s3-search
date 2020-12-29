package com.van.logging.aws;

import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class AwsClientHelpers {

    static AWSCredentialsProvider getCredentialsProvider(
        final String accessKey, final String secretKey, final String sessionToken
    ) {
        AWSCredentialsProvider credProvider;
        if ((null != accessKey) && (null != secretKey)) {
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            if (sessionToken != null) {
                credentials = new BasicSessionCredentials(accessKey, secretKey, sessionToken);
            }
            AWSCredentials finalCredentials = credentials;
            credProvider = new AWSCredentialsProviderChain(
                // If the user took the pains to construct us with the access
                // credentials, give them priority over the defaults from the
                // the more general environment
                new AWSCredentialsProvider() {
                    public AWSCredentials getCredentials() {
                        return finalCredentials;
                    }

                    public void refresh() {
                    }
                },
                new DefaultAWSCredentialsProviderChain()
            );
        } else {
            credProvider = new DefaultAWSCredentialsProviderChain();
        }
        return credProvider;
    }

    static AwsClientBuilder getS3ClientBuilder() {
        return AmazonS3ClientBuilder.standard();
    }
    
    static AwsClientBuilder getS3ClientBuilderwithEnablePathStyleAccess() {
        return AmazonS3ClientBuilder.standard().enablePathStyleAccess();
    }
    

    /**
     * Creates an AmazonS3Client using the optional configuration information provided. If accessKey and
     * secretKey are null, then the default credential configuration for AWS is used.
     *
     * Similarly, if region is null, then the default region is used.
     *
     * See https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html and
     * https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html
     *
     * @param accessKey optional access key for S3 access.
     * @param secretKey optional secret key for S3 access.
     * @param sessionToken optional session token for S3 access.
     * @param region optional region to use.
     * @param serviceEndpoint optional service endpoint to use when initializing the S3 Client.
     * @param signingRegion optional signing region to use when initializing the S3 Client.
     *
     * @return the client class to use for S3 access.
     */
    public static AmazonS3 buildClient(
        String accessKey, String secretKey, String sessionToken,
        Region region,
        String serviceEndpoint, String signingRegion, boolean pathStyleAccess
    ) {
        AWSCredentialsProvider credentialsProvider =
            getCredentialsProvider(accessKey, secretKey, sessionToken);
        AwsClientBuilder builder = null;
        if (pathStyleAccess){
        	if (builder == null)
        		builder = getS3ClientBuilderwithEnablePathStyleAccess();
        }else
        {
        	if (builder == null)
        		builder=getS3ClientBuilder();
        }
        builder = builder.withCredentials(credentialsProvider);
        if (region != null) {
            builder = builder.withRegion(region.getName());
        }
        if (serviceEndpoint != null) {
            // I don't know if signingRegion is required or if null is acceptable in practice, so I'm going to allow
            // it and install the EndpointConfiguration as long as the serviceEndpoint is defined.
            // Disclaimer: I have not tested this; I'm going strictly by docs like:
            // https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/client/builder/AwsClientBuilder.EndpointConfiguration.html
            // https://www.digitalocean.com/community/questions/how-to-use-digitalocean-spaces-with-the-aws-s3-sdks
            builder = builder.withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, signingRegion)
            );
        }
        return (AmazonS3) builder.build();
    }
}
