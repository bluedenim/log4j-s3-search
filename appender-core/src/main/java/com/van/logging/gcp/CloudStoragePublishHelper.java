package com.van.logging.gcp;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;
import com.van.logging.AbstractFilePublishHelper;
import com.van.logging.IStorageDestinationAdjuster;
import com.van.logging.PublishContext;
import com.van.logging.VansLogger;
import com.van.logging.utils.PublishHelperUtils;
import com.van.logging.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;

/**
 * Publish helper to publish content to Google Cloud Platform's Storage service.
 */
public class CloudStoragePublishHelper extends AbstractFilePublishHelper {
    private final CloudStorageConfiguration configuration;
    private final IStorageDestinationAdjuster storageDestinationAdjuster;

    public CloudStoragePublishHelper(
        CloudStorageConfiguration configuration,
        IStorageDestinationAdjuster storageDestinationAdjuster,
        boolean verbose
    ) {
        super(configuration.isCompressionEnabled(), verbose);
        this.configuration = configuration;
        this.storageDestinationAdjuster = storageDestinationAdjuster;
    }

    static String getBlobName(PublishContext context, String path, CloudStorageConfiguration configuration) {
        String blobName = String.format("%s%s", path, context.getCacheName());
        if (configuration.isCompressionEnabled() && configuration.isBlobNameGzSuffixEnabled()) {
            blobName = String.format("%s.gz", blobName);
        }
        return blobName;
    }

    @Override
    protected void publishFile(File file, PublishContext context) throws Exception {
        String bucketName = configuration.getBucketName();
        Storage storage = StorageOptions.getDefaultInstance().getService();
        Bucket bucket = storage.get(bucketName);
        if (null == bucket) {
            bucket = storage.create(BucketInfo.of(bucketName));
        }
        String path = StringUtils.addTrailingIfNeeded(
            PublishHelperUtils.adjustStoragePathIfNecessary(
                configuration.getBlobNamePrefix(),
                storageDestinationAdjuster
            ),
            "/"
        );
        String blobName = getBlobName(context, path, configuration);
        if (this.verbose) {
            VansLogger.logger.debug(String.format("Publishing %s to GCS blob (bucket=%s; blob=%s):",
                file.getAbsolutePath(), bucketName, blobName));
        }
        BlobId blobId = BlobId.of(bucketName, blobName);
        String contentType = configuration.isCompressionEnabled() ? "application/gzip" : "text/plain";
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
        uploadToStorage(storage, file, blobInfo);
    }

    private void uploadToStorage(Storage storage, File uploadFrom, BlobInfo blobInfo) throws IOException {
        // From: https://stackoverflow.com/questions/53628684/how-to-upload-a-large-file-into-gcp-cloud-storage

        if (uploadFrom.length() < 1_000_000) {
            storage.create(blobInfo, Files.readAllBytes(uploadFrom.toPath()));
        } else {
            // For big files:
            // When content is not available or large (1MB or more) it is recommended to write it in chunks via
            // the blob's channel writer.
            try (WriteChannel writer = storage.writer(blobInfo)) {
                byte[] buffer = new byte[10_240];
                try (InputStream input = Files.newInputStream(uploadFrom.toPath())) {
                    int limit;
                    while ((limit = input.read(buffer)) >= 0) {
                        writer.write(ByteBuffer.wrap(buffer, 0, limit));
                    }
                }

            }
        }
    }
}
