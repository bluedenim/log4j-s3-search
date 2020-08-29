package com.van.logging.azure;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.blob.*;
import com.van.logging.AbstractFilePublishHelper;
import com.van.logging.IStorageDestinationAdjuster;
import com.van.logging.PublishContext;
import com.van.logging.utils.PublishHelperUtils;
import com.van.logging.utils.StringUtils;

import java.io.File;


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

    @Override
    protected void publishFile(File file, PublishContext context) throws Exception {
        String connectionString = blobConfiguration.getStorageConnectionString();

        if (null == connectionString || connectionString.isEmpty()) {
            connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
        }
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(blobConfiguration.getContainerName());
        boolean created = container.createIfNotExists(
            BlobContainerPublicAccessType.CONTAINER, new BlobRequestOptions(), new OperationContext());

        String path = StringUtils.addTrailingIfNeeded(
            PublishHelperUtils.adjustStoragePathIfNecessary(
                blobConfiguration.getBlobNamePrefix(),
                storageDestinationAdjuster
            ),
            "/"
        );
        String blobName = String.format("%s%s", path, context.getCacheName());
        if (this.verbose) {
            System.out.println(String.format("Publishing %s to Azure blob (container=%s; blob=%s):",
                file.getAbsolutePath(), blobConfiguration.getContainerName(), blobName));
        }

        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        if (this.blobConfiguration.isCompressionEnabled()) {
            blob.getProperties().setContentType("application/gzip");
        } else {
            blob.getProperties().setContentType("text/plain");
        }
        blob.uploadFromFile(file.getAbsolutePath());
        if (this.verbose) {
            System.out.println(String.format("Publishing to Azure blob %s done.", blobName));
        }
    }
}
