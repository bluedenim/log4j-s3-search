package com.van.logging.aws;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsSyncClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.lang.reflect.Constructor;

/**
 * Builder to build various AWS clients.  In addition to using the default
 * credential providers that the AWS JDK supports, this builder also allows
 * passing in the credentials for applications that prefer to do so.
 *
 * @author vly
 *
 */
public class AwsClientBuilder {
    private final String accessKey;
    private final String secretKey;
    private final Region region;

    /**
     * Instantiate a builder with the credentials and region specified (using the {@link Regions} type).
     *
     * @param regions the region in which to create clients for
     * @param accessKey the access key to use for credentials
     * @param secretKey the secret key to use for credentials
     */
    public AwsClientBuilder(Regions regions, String accessKey, String secretKey) {
        this(Region.getRegion(regions), accessKey, secretKey);
    }

    /**
     * Instantiate a builder with the credentials and region specified (using the {@link Region} type).
     *
     * @param region the region in which to create clients for
     * @param accessKey the access key to use for credentials
     * @param secretKey the secret key to use for credentials
     */
    public AwsClientBuilder(Region region, String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
    }

    /**
     * Instantiate a builder using the default AWS credential sources
     *
     * @param region the region in which to create clients for
     */
    public AwsClientBuilder(Regions region) {
        this(region, null, null);
    }

    /**
     * Instantiate a builder using the default AWS credential sources
     *
     * @param region the region in which to create clients for
     */
    public AwsClientBuilder(Region region) {
        this(region, null, null);
    }

    /**
     * Default ctor with no Region preference
     */
    public AwsClientBuilder() {
        this((Region)null);
    }

    /**
     * Build a service client class using the parameters set up for the builder
     *
     * @param clientClass the class of the service client to build
     *
     * @return an instance of the service client
     */
    @SuppressWarnings( "deprecation" )
    public <T extends AmazonWebServiceClient> AmazonWebServiceClient build(Class<T> clientClass) {
        AmazonWebServiceClient client;
        try {
            client = (AmazonWebServiceClient)instantiateClient(clientClass);
            if (null != region) {
                // TODO: Use the AWS SDK's AmazonS3ClientBuilder. With the newest hierarchy of
                // builders, we may not need this class safe the few helpers to inject
                // configured credentials.
                client.setRegion(region);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot build client", e);
        }
        return client;
    }

    Object instantiateClient(Class clientClass) throws Exception {
        @SuppressWarnings("unchecked")
        Constructor ctor = clientClass.getConstructor(AWSCredentialsProvider.class);
        AWSCredentialsProvider credentialsProvider = getCredentialsProvider();
        /* System.out.println(String.format("Initializing %s with ID %s...",
            clientClass.getName(), credentialsProvider.getCredentials().getAWSAccessKeyId())); */
        return ctor.newInstance(credentialsProvider);
    }

    AWSCredentialsProvider getCredentialsProvider() {
        AWSCredentialsProvider credProvider;
        if ((null != accessKey) && (null != secretKey)) {
            credProvider = new AWSCredentialsProviderChain(
                // If the user took the pains to construct us with the access
                // credentials, give them priority over the defaults from the
                // the more general environment
                new AWSCredentialsProvider() {
                    public AWSCredentials getCredentials() {
                        return new BasicAWSCredentials(accessKey, secretKey);
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
}
