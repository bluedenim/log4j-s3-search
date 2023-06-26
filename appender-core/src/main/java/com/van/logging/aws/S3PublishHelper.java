package com.van.logging.aws;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.van.logging.AbstractFilePublishHelper;
import com.van.logging.IStorageDestinationAdjuster;
import com.van.logging.PublishContext;
import com.van.logging.VansLogger;
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
    private final boolean isCompressionEnabled;
    private final boolean keyGzSuffix;
    private final StorageClass storageClass;

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
        this.isCompressionEnabled = s3.isCompressionEnabled();
        this.keyGzSuffix = s3.isKeyGzSuffixEnabled();
        this.storageClass = s3.getStorageClass();
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

    static String getKey(PublishContext context, String path, boolean isCompressionEnabled, boolean keyGzSuffix) {
        String key = String.format("%s%s", path, context.getCacheName());
        if (isCompressionEnabled && keyGzSuffix) {
            key = String.format("%s.gz", key);
        }
        return key;
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
        String key = getKey(context, path, isCompressionEnabled, keyGzSuffix);
        if (this.verbose) {
            VansLogger.logger.debug(String.format("Publishing to S3 (bucket=%s; key=%s):", bucket, key));
        }

        try {
            if (VansLogger.logger.isDebugEnabled()) {
                VansLogger.logger.debug(String.format("Publishing content of %s to S3.", file));
            }
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            metadata.setContentType(ContentType.DEFAULT_BINARY.getMimeType());
            // Currently, only SSE_S3 is supported
            if ((sseConfig != null) && (sseConfig.getKeyType() == S3Configuration.SSEType.SSE_S3)) {
                // https://docs.aws.amazon.com/AmazonS3/latest/dev/SSEUsingJavaSDK.html
                metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            }

            final PutObjectRequest por = new PutObjectRequest(bucket, key, file);

            if (cannedAcl != null) por.setCannedAcl(cannedAcl);
            if (storageClass != null) por.setStorageClass(storageClass);

            por.setMetadata(metadata);

            PutObjectResult result = client.putObject(por);
            if (VansLogger.logger.isDebugEnabled()) {
                VansLogger.logger.debug(String.format("Content MD5: %s", result.getContentMd5()));
            }
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Cannot publish to S3: %s", ex.getMessage()), ex);
        }
    }
}
