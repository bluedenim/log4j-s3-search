package com.van.logging.aws;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.van.logging.AbstractFilePublishHelper;
import com.van.logging.PublishContext;
import org.apache.http.entity.ContentType;

import java.io.*;

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

    private volatile boolean bucketExists = false;

    public S3PublishHelper(S3Configuration s3) {
        super(s3.isCompressionEnabled());
        this.client = (AmazonS3Client)buildClient(
            s3.getAccessKey(), s3.getSecretKey(), s3.getSessionToken(),
            s3.getRegion(),
            s3.getServiceEndpoint(), s3.getSigningRegion()
        );

        this.bucket = s3.getBucket().toLowerCase();
        String path = s3.getPath();
        if (!path.endsWith("/")) {
            this.path = path + "/";
        } else {
            this.path = path;
        }
        this.sseConfig = s3.getSseConfiguration();
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
        String key = String.format("%s%s", this.path, context.getCacheName());
		/* System.out.println(String.format("Publishing to S3 (bucket=%s; key=%s):",
			bucket, key)); */

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

            PutObjectRequest por = new PutObjectRequest(bucket, key, file);
            por.setMetadata(metadata);

            PutObjectResult result = client.putObject(por);
            // System.out.println(String.format("Content MD5: %s", result.getContentMd5()));
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Cannot publish to S3: %s", ex.getMessage()), ex);
        }
    }
}
