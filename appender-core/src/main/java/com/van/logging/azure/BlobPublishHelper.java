package com.van.logging.azure;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.blob.*;
import com.van.logging.AbstractFilePublishHelper;
import com.van.logging.PublishContext;

import java.io.File;


/**
 * Implementation to publish logs to Azure's Blob Storage.
 * <br>
 * Tags are currently ignored by the publisher.
 */
public class BlobPublishHelper extends AbstractFilePublishHelper {

    private final BlobConfiguration blobConfiguration;

    public BlobPublishHelper(BlobConfiguration blobConfiguration) {
        super(blobConfiguration.isCompressionEnabled());
        this.blobConfiguration = blobConfiguration;
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

        String prefix = "";
        if (null != blobConfiguration.getBlobNamePrefix()) {
            prefix = blobConfiguration.getBlobNamePrefix();
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        String blobName = String.format("%s%s", prefix, context.getCacheName());
		System.out.println(String.format("Publishing %s to Azure blob (container=%s; blob=%s):",
			file.getAbsolutePath(), blobConfiguration.getContainerName(), blobName));

        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        if (this.blobConfiguration.isCompressionEnabled()) {
            blob.getProperties().setContentType("application/gzip");
        } else {
            blob.getProperties().setContentType("text/plain");
        }
        blob.uploadFromFile(file.getAbsolutePath());
        System.out.println(String.format("Publishing to Azure blob %s done.", blobName));
    }
}
