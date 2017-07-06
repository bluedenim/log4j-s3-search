package com.van.logging.aws;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.van.logging.Event;
import com.van.logging.IPublishHelper;
import com.van.logging.PublishContext;
import org.apache.http.entity.ContentType;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

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
    private volatile StringBuilder stringBuilder;

    public S3PublishHelper(AmazonS3Client client, String bucket, String path) {
        this.client = client;
        this.bucket = bucket.toLowerCase();
        if (!path.endsWith("/")) {
            this.path = path + "/";
        } else {
            this.path = path;
        }
    }

    public void publish(PublishContext context, int sequence, Event event) {
        stringBuilder.append(event.getMessage())
            .append(LINE_SEPARATOR);
    }

    public void start(PublishContext context) {
        stringBuilder = new StringBuilder();
        if (!bucketExists) {
            bucketExists = client.doesBucketExist(bucket);
            if (!bucketExists) {
                client.createBucket(bucket);
                bucketExists = true;
            }
        }
    }

    public void end(PublishContext context) {
        String key = String.format("%s%s", path, context.getCacheName());
		/* System.out.println(String.format("Publishing to S3 (bucket=%s; key=%s):",
			bucket, key)); */

        String data = stringBuilder.toString();
		/* System.out.println(data); */
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            byte bytes[] = data.getBytes("UTF-8");
            metadata.setContentLength(bytes.length);
            metadata.setContentType(ContentType.TEXT_PLAIN.getMimeType());
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            PutObjectResult result = client.putObject(bucket,
                key, is, metadata);
			/* System.out.println(String.format("Content MD5: %s",
				result.getContentMd5())); */
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        stringBuilder = null;
    }

}
