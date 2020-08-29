package com.van.logging.log4j2;

import com.van.logging.*;
import com.van.logging.aws.S3Configuration;
import com.van.logging.aws.S3PublishHelper;
import com.van.logging.azure.BlobConfiguration;
import com.van.logging.azure.BlobPublishHelper;
import com.van.logging.elasticsearch.ElasticsearchConfiguration;
import com.van.logging.elasticsearch.ElasticsearchPublishHelper;
import com.van.logging.gcp.CloudStorageConfiguration;
import com.van.logging.gcp.CloudStoragePublishHelper;
import com.van.logging.solr.SolrConfiguration;
import com.van.logging.solr.SolrPublishHelper;
import com.van.logging.utils.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Log4j2AppenderBuilder extends org.apache.logging.log4j.core.appender.AbstractAppender.Builder
    implements org.apache.logging.log4j.core.util.Builder<Log4j2Appender> {

    // general properties
    @PluginBuilderAttribute
    private boolean verbose = false;

    @PluginBuilderAttribute
    private String tags;

    @PluginBuilderAttribute
    private int stagingBufferSize = 25;

    @PluginBuilderAttribute
    private int stagingBufferAge = 0;

    // S3 properties
    @PluginBuilderAttribute
    private String s3Bucket;

    @PluginBuilderAttribute
    private String s3Region;

    @PluginBuilderAttribute
    private String s3Path;

    @PluginBuilderAttribute
    private String s3AwsKey;

    @PluginBuilderAttribute
    private String s3AwsSecret;

    @PluginBuilderAttribute
    private String s3AwsSessionToken = null;

    @PluginBuilderAttribute
    private String s3ServiceEndpoint;

    @PluginBuilderAttribute
    private String s3SigningRegion;

    @PluginBuilderAttribute
    private String s3Compression;

    @PluginBuilderAttribute
    private String s3SseKeyType;

    @PluginBuilderAttribute
    private String s3CannedAcl;

    // Azure blob properties
    @PluginBuilderAttribute
    private String azureStorageConnectionString;

    @PluginBuilderAttribute
    private String azureBlobContainer;

    @PluginBuilderAttribute
    private String azureBlobNamePrefix;

    @PluginBuilderAttribute
    private String azureBlobCompressionEnabled;

    // GCP Storage properties
    @PluginBuilderAttribute
    private String gcpStorageBucket;

    @PluginBuilderAttribute
    private String gcpStorageBlobNamePrefix;

    @PluginBuilderAttribute
    private String gcpStorageCompressionEnabled;

    // Solr properties
    @PluginBuilderAttribute
    private String solrUrl;

    // Elasticsearch properties
    @PluginBuilderAttribute
    private String elasticsearchCluster;

    @PluginBuilderAttribute
    private String elasticsearchIndex;

    @PluginBuilderAttribute
    private String elasticsearchType;

    @PluginBuilderAttribute
    private String elasticsearchHosts;


    @Override
    @SuppressWarnings("unchecked")
    public Log4j2Appender build() {
        try {
            String cacheName = UUID.randomUUID().toString().replaceAll("-","");
            LoggingEventCache<Event> cache = new LoggingEventCache<>(
                cacheName, createCacheMonitor(), createCachePublisher());
            return installFilter(new Log4j2Appender(
                    getName(), getFilter(), getLayout(),
                    true, cache));
        } catch (Exception e) {
            throw new RuntimeException("Cannot build appender due to errors", e);
        }
    }

    Log4j2Appender installFilter(Log4j2Appender appender) {
        appender.addFilter(new AbstractFilter() {
            @Override
            public Result filter(final LogEvent event) {
                // To prevent infinite looping, we filter out events from
                // the publishing thread
                Result decision = Result.NEUTRAL;
                if (LoggingEventCache.PUBLISH_THREAD_NAME.equals(event.getThreadName())) {
                    decision = Result.DENY;
                }
                return decision;
            }});
        return appender;
    }

    Optional<S3Configuration> getS3ConfigIfEnabled() {
        Optional<S3Configuration> s3Config = Optional.empty();
        if (StringUtils.isTruthy(s3Bucket)) {
            S3Configuration config = new S3Configuration();
            config.setBucket(s3Bucket);
            config.setPath(s3Path);
            config.setRegion(s3Region);
            config.setAccessKey(s3AwsKey);
            config.setSecretKey(s3AwsSecret);
            config.setSessionToken(s3AwsSessionToken);
            config.setServiceEndpoint(s3ServiceEndpoint);
            config.setSigningRegion(s3SigningRegion);
            if (StringUtils.isTruthy(s3CannedAcl)) {
                try {
                    config.setCannedAclFromValue(s3CannedAcl);
                } catch (IllegalArgumentException ex) {
                    System.err.println(String.format("Ignoring unrecognized canned ACL value %s", s3CannedAcl));
                }
            }

            S3Configuration.S3SSEConfiguration sseConfig = null;
            if (s3SseKeyType != null) {
                sseConfig = new S3Configuration.S3SSEConfiguration(
                        S3Configuration.SSEType.valueOf(s3SseKeyType),
                        null
                );
            }
            config.setSseConfiguration(sseConfig);
            s3Config = Optional.of(config);
        }
        return s3Config;
    }

    Optional<BlobConfiguration> getBlobConfigurationIfEnabled() {
        Optional<BlobConfiguration> blobConfiguration = Optional.empty();
        if (StringUtils.isTruthy(this.azureBlobContainer)) {
            BlobConfiguration config = new BlobConfiguration();
            config.setStorageConnectionString(this.azureStorageConnectionString);
            config.setContainerName(this.azureBlobContainer);
            if (null != this.azureBlobNamePrefix) {
                config.setBlobNamePrefix(this.azureBlobNamePrefix);
            }
            config.setCompressionEnabled(Boolean.parseBoolean(this.azureBlobCompressionEnabled));
            blobConfiguration = Optional.of(config);
        }
        return blobConfiguration;
    }

    Optional<CloudStorageConfiguration> getCloudStorageConfigurationIfEnabled() {
        Optional<CloudStorageConfiguration> cloudStorageConfiguration = Optional.empty();
        if (StringUtils.isTruthy(this.gcpStorageBucket)) {
            CloudStorageConfiguration config = new CloudStorageConfiguration();
            config.setBucketName(this.gcpStorageBucket);
            config.setBlobNamePrefix(this.gcpStorageBlobNamePrefix);
            config.setCompressionEnabled(Boolean.parseBoolean(this.gcpStorageCompressionEnabled));
            cloudStorageConfiguration = Optional.of(config);
        }
        return cloudStorageConfiguration;
    }

    static Optional<SolrConfiguration> getSolrConfigurationIfEnabled(String solrUrl) {
        SolrConfiguration config = null;
        if (null != solrUrl) {
            config = new SolrConfiguration();
            config.setUrl(solrUrl);
        }
        return Optional.ofNullable(config);
    }

    static Optional<ElasticsearchConfiguration> getElasticsearchConfigIfEnabled(String elasticsearchHosts) {
        ElasticsearchConfiguration config = null;
        if (null != elasticsearchHosts) {
            config = new ElasticsearchConfiguration();
            String hostArray[] = elasticsearchHosts.split("[;\\s,]");
            for (String entry: hostArray) {
                config.addHost(entry);
            }
        }
        return Optional.ofNullable(config);
    }

    IBufferPublisher<Event> createCachePublisher() throws UnknownHostException {

        java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
        String hostName = addr.getHostName();
        BufferPublisher<Event> publisher = new BufferPublisher<Event>(hostName, parseTags(tags));
        PatternedPathAdjuster pathAdjuster = new PatternedPathAdjuster();

        getS3ConfigIfEnabled().ifPresent(config -> {
            if (verbose) {
                System.out.println(String.format(
                    "Registering AWS S3 publish helper -> %s", config));
            }
            publisher.addHelper(new S3PublishHelper(config, pathAdjuster, verbose));
        });

        getBlobConfigurationIfEnabled().ifPresent(config -> {
            if (verbose) {
                System.out.println(String.format("Registering Azure Blob Storage publish helper -> %s", config));
            }
            publisher.addHelper(new BlobPublishHelper(config, pathAdjuster, verbose));
        });

        getCloudStorageConfigurationIfEnabled().ifPresent(config -> {
            if (verbose) {
                System.out.println(String.format("Registering Google Cloud Storage publish helper -> %s", config));
            }
            publisher.addHelper(new CloudStoragePublishHelper(config, pathAdjuster, verbose));
        });

        getSolrConfigurationIfEnabled(solrUrl).ifPresent(config -> {
            if (verbose) {
                System.out.println(String.format(
                    "Registering SOLR publish helper -> %s", config));
            }
            publisher.addHelper(new SolrPublishHelper(config.getUrl()));
        });

        getElasticsearchConfigIfEnabled(elasticsearchHosts).ifPresent(config -> {
            if (verbose) {
                System.out.println(String.format(
                    "Registering Elasticsearch publish helper -> %s", config));
            }
            publisher.addHelper(new ElasticsearchPublishHelper(config));
        });

        return publisher;
    }

    String[] parseTags(String tags) {
        Set<String> parsedTags = null;
        if (null != tags) {
            parsedTags = Stream.of(tags.split("[,;]"))
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toSet());
        } else {
            parsedTags = Collections.emptySet();
        }
        return parsedTags.toArray(new String[] {});
    }

    IBufferMonitor<Event> createCacheMonitor() {
        IBufferMonitor<Event> monitor = new CapacityBasedBufferMonitor<Event>(stagingBufferSize);
        if (0 < stagingBufferAge) {
            monitor = new TimePeriodBasedBufferMonitor<Event>(stagingBufferAge);
        }
        if (verbose) {
            System.out.println(String.format("Using cache monitor: %s", monitor.toString()));
        }
        return monitor;
    }
}
