package com.van.logging.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.AccessTier;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.van.logging.AbstractFilePublishHelper;
import com.van.logging.IStorageDestinationAdjuster;
import com.van.logging.PublishContext;
import com.van.logging.VansLogger;
import com.van.logging.utils.PublishHelperUtils;
import com.van.logging.utils.StringUtils;

import java.io.File;
import java.time.Duration;
import java.util.Map;


/**
 * Implementation to publish logs to Azure's Blob Storage.
 * <br>
 * Tags are currently ignored by the publisher.
 */
public class BlobPublishHelper extends AbstractFilePublishHelper {

    private final BlobConfiguration blobConfiguration;
    private final IStorageDestinationAdjuster storageDestinationAdjuster;

    public BlobPublishHelper(
        BlobConfiguration blobConfiguration,
        IStorageDestinationAdjuster storageDestinationAdjuster,
        boolean verbose
    ) {
        super(blobConfiguration.isCompressionEnabled(), verbose);
        this.blobConfiguration = blobConfiguration;
        this.storageDestinationAdjuster = storageDestinationAdjuster;
    }

    static String getBlobName(PublishContext context, String path, BlobConfiguration blobConfiguration) {
        String blobName = String.format("%s%s", path, context.getCacheName());
        if (blobConfiguration.isCompressionEnabled() && blobConfiguration.isBlobNameGzSuffixEnabled()) {
            blobName = String.format("%s.gz", blobName);
        }
        return blobName;
    }

    @Override
    protected void publishFile(File file, PublishContext context) throws Exception {
        String connectionString = blobConfiguration.getStorageConnectionString();

        if (null == connectionString || connectionString.isEmpty()) {
            connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
        }
        // Source: https://learn.microsoft.com/en-us/azure/storage/blobs/storage-quickstart-blobs-java#upload-blobs-to-a-container
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();

        BlobContainerClient blobContainerClient = blobServiceClient.createBlobContainerIfNotExists(
            blobConfiguration.getContainerName()
        );

        String path = StringUtils.addTrailingIfNeeded(
            PublishHelperUtils.adjustStoragePathIfNecessary(
                blobConfiguration.getBlobNamePrefix(),
                storageDestinationAdjuster
            ),
            "/"
        );
        String blobName = getBlobName(context, path, blobConfiguration);
        if (this.verbose) {
            VansLogger.logger.debug(String.format("Publishing %s to Azure blob (container=%s; blob=%s):",
                file.getAbsolutePath(), blobConfiguration.getContainerName(), blobName));
        }
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        BlobHttpHeaders headers = new BlobHttpHeaders();
        if (this.blobConfiguration.isCompressionEnabled()) {
            headers.setContentType("application/gzip");
        } else {
            headers.setContentType("text/plain");
        }
        // The following is a bit odd by setting almost everything as null except the headers, but that
        // is what BlobClient.uploadFromFile(String) does.
        blobClient.uploadFromFile(
            file.getAbsolutePath(), (ParallelTransferOptions)null, headers,
            (Map<String,String>)null, (AccessTier)null, (BlobRequestConditions)null, (Duration)null
        );

        if (this.verbose) {
            VansLogger.logger.debug(String.format("Publishing to Azure blob %s done.", blobName));
        }
    }
}
