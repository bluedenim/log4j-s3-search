package com.van.logging.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.van.logging.AbstractFilePublishHelper;
import com.van.logging.IStorageDestinationAdjuster;
import com.van.logging.PublishContext;
import com.van.logging.utils.PublishHelperUtils;
import com.van.logging.utils.StringUtils;
import org.apache.http.entity.ContentType;

import java.io.File;

import static com.van.logging.aws.AwsClientHelpers.buildClient;

/**
 * Implementation to publish log events to S3.
 * <br>
 * <em>NOTES</em>:
 * <ul>
 * <li>If the access key and secret key are provided, they will be preferred over
 * whatever default setting (e.g. ~/.aws/credentials or
 * %USERPROFILE%\.aws\credentials) is in place for the
 * runtime environment.</li>
 * <li>Tags are currently ignored by the S3 publisher.</li>
 * </ul>
 *
 * @author vly
 *
 */
public class S3PublishHelper extends AbstractFilePublishHelper {

    private final AmazonS3Client client;
    private final String bucket;
    private final String path;
    private final S3Configuration.S3SSEConfiguration sseConfig;
    private final CannedAccessControlList cannedAcl;
    private final IStorageDestinationAdjuster storageDestinationAdjuster;

    private volatile boolean bucketExists = false;

    public S3PublishHelper(
        S3Configuration s3,
        IStorageDestinationAdjuster storageDestinationAdjuster,
        boolean verbose
    ) {
        super(s3.isCompressionEnabled(), verbose);
        this.client = (AmazonS3Client)buildClient(
            s3.getAccessKey(), s3.getSecretKey(), s3.getSessionToken(),
            s3.getRegion(),
            s3.getServiceEndpoint(), s3.getSigningRegion(),
            s3.isPathStyleAccess()
        );
        this.bucket = s3.getBucket().toLowerCase();
        this.storageDestinationAdjuster = storageDestinationAdjuster;
        this.path = s3.getPath();
        this.sseConfig = s3.getSseConfiguration();
        this.cannedAcl = s3.getCannedAcl();
    }

    @Override
    public void start(PublishContext context) {
        super.start(context);
        try {
            if (!bucketExists) {
                bucketExists = client.doesBucketExist(bucket);
                if (!bucketExists) {
                    client.createBucket(bucket);
                    bucketExists = true;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Cannot start publishing: %s", ex.getMessage()), ex);
        }
    }

    @Override
    protected void publishFile(File file, PublishContext context) {
        String path = StringUtils.addTrailingIfNeeded(
            PublishHelperUtils.adjustStoragePathIfNecessary(
                this.path,
                storageDestinationAdjuster
            ),
            "/"
        );
        String key = String.format("%s%s", path, context.getCacheName());
		/* System.out.println(String.format("Publishing to S3 (bucket=%s; key=%s):",
			bucket, path)); */

        try {
            // System.out.println(String.format("Publishing content of %s to S3.", tempFile));
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            metadata.setContentType(ContentType.DEFAULT_BINARY.getMimeType());
            // Currently, only SSE_S3 is supported
            if ((sseConfig != null) && (sseConfig.getKeyType() == S3Configuration.SSEType.SSE_S3)) {
                // https://docs.aws.amazon.com/AmazonS3/latest/dev/SSEUsingJavaSDK.html
                metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            }

            PutObjectRequest por;
            if (cannedAcl != null)
                por = new PutObjectRequest(bucket, key, file).withCannedAcl(cannedAcl);
            else
                por = new PutObjectRequest(bucket, key, file);
            por.setMetadata(metadata);

            PutObjectResult result = client.putObject(por);
            // System.out.println(String.format("Content MD5: %s", result.getContentMd5()));
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Cannot publish to S3: %s", ex.getMessage()), ex);
        }
    }

    public static void main(String[] args) {
        AWSCredentials credentials = new BasicAWSCredentials("minio", "minio123");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");

        AmazonS3 s3Client = AmazonS3ClientBuilder
            .standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:9000", Regions.US_EAST_1.name()))
            .withPathStyleAccessEnabled(true)
            .withClientConfiguration(clientConfiguration)
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .build();


        s3Client = buildClient("minio", "minio123",
            null,  null, "http://localhost:9000", "us-east-2", true);
        try {
            String bucketName = "test-2";
            String keyName = "minio-sa";
            System.out.println("Uploading a new object to S3 from a file\n");
            File file = new File("/Users/davinchia/Desktop/minio-standalone.yml");
            // Upload file
            s3Client.putObject(new PutObjectRequest(bucketName, keyName, file));

            // Download file
            GetObjectRequest rangeObjectRequest = new GetObjectRequest(bucketName, keyName);
            S3Object objectPortion = s3Client.getObject(rangeObjectRequest);
            System.out.println("Printing bytes retrieved:");
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " + "means your request made it "
                + "to Amazon S3, but was rejected with an error response" + " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());

        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " + "means the client encountered " + "an internal error while trying to "
                + "communicate with S3, " + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());

        }
    }
}
