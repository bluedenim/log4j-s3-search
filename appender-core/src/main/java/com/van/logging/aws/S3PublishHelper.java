package com.van.logging.aws;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.van.logging.Event;
import com.van.logging.IPublishHelper;
import com.van.logging.PublishContext;
import org.apache.http.entity.ContentType;

import java.io.*;

/**
 * Implementation to publish log events to S3.
 * <br>
 * These Log4j logger parameters configure the S3 publisher:
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
public class S3PublishHelper implements IPublishHelper<Event> {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String S3ERRCODE_BUCKETALREADYOWNEDBYYOU = "BucketAlreadyOwnedByYou";

    private final AmazonS3Client client;
    private final String bucket;
    private final String path;

    private volatile boolean bucketExists = false;

    private File tempFile;
    private Writer outputWriter;


    public S3PublishHelper(AmazonS3Client client, String bucket, String path) {
        this.client = client;
        this.bucket = bucket.toLowerCase();
        if (!path.endsWith("/")) {
            this.path = path + "/";
        } else {
            this.path = path;
        }
    }

    public void start(PublishContext context) {
        try {
            tempFile = File.createTempFile("s3Publish", null);
            outputWriter = new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(tempFile)));
//            System.out.println(
//                String.format("Collecting content into %s before sending to S3.", tempFile));

            if (!bucketExists) {
                bucketExists = client.doesBucketExist(bucket);
                if (!bucketExists) {
                    client.createBucket(bucket);
                    bucketExists = true;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Cannot start publishing.", ex);
        }
    }

    public void publish(PublishContext context, int sequence, Event event) {
        try {
            outputWriter.write(event.getMessage());
            outputWriter.write(LINE_SEPARATOR);
        } catch (Exception ex) {
            throw new RuntimeException(
                String.format("Cannot collect event %s.", event), ex);
        }
    }


    public void end(PublishContext context) {
        String key = String.format("%s%s", path, context.getCacheName());
		/* System.out.println(String.format("Publishing to S3 (bucket=%s; key=%s):",
			bucket, key)); */

        try {
            if (null != outputWriter) {
                outputWriter.close();
                outputWriter = null;
//                System.out.println(String.format("Publishing content of %s to S3.", tempFile));
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(tempFile.length());
                metadata.setContentType(ContentType.DEFAULT_BINARY.getMimeType());

                PutObjectRequest por = new PutObjectRequest(bucket, key, tempFile);
                por.setMetadata(metadata);

                PutObjectResult result = client.putObject(por);
                /* System.out.println(String.format("Content MD5: %s",
                    result.getContentMd5())); */
            }
        } catch (UnsupportedEncodingException e) {
        } catch (Exception ex) {
            throw new RuntimeException("Cannot publish to S3.", ex);
        } finally {
            if (null != tempFile) {
                try {
                    tempFile.delete();
                    tempFile = null;
                } catch (Exception ex) {
                }
            }
        }
    }

}
