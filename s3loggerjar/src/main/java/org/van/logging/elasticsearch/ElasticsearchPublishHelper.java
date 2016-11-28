package org.van.logging.elasticsearch;

import org.apache.log4j.spi.LoggingEvent;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.van.logging.PublishContext;
import org.van.logging.log4j.IPublishHelper;

import java.net.InetAddress;
import java.util.Date;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Elasticsearch implementation of IPublishHelper to publish logs into Elasticsearch
 * Created by vly on 11/26/2016.
 */
public class ElasticsearchPublishHelper implements IPublishHelper {

    private final ElasticsearchConfiguration configuration;

    private TransportClient transport;
    private BulkRequestBuilder builder;
    private int offset;
    private Date timeStamp;

    public ElasticsearchPublishHelper(ElasticsearchConfiguration configuration) {
        this.configuration = configuration;
    }

    private TransportClient getTransportClient(ElasticsearchConfiguration config) {
        Settings settings = Settings.builder()
            .put("cluster.name", config.getClusterName())
            .build();
        final PreBuiltTransportClient client = new PreBuiltTransportClient(settings);
        config.iterateHosts((host, port) -> {
            try {
                client.addTransportAddress(
                    new InetSocketTransportAddress(InetAddress.getByName(host), port)
                );
            } catch (Exception e) {
                System.err.println(String.format(
                    "Cannot add Elasticsearch host %s(%d): %s",
                    host, port, e.getMessage()));
            }
        });
        return client;
    }

    @Override
    public void start(PublishContext context) {
        offset = 0;
        timeStamp = new Date();
        transport = getTransportClient(this.configuration);
        builder = transport.prepareBulk();
    }

    @Override
    public void publish(PublishContext context, int sequence, LoggingEvent event) {
        try {
            String id = String.format("%s-%s-%016d", context.getCacheName(),
                context.getHostName(), offset);
            XContentBuilder contentBuilder = jsonBuilder()
                .startObject()
                    .field("timestamp", timeStamp)
                    .field("type", event.getLevel().toString())
                    .field("hostname", context.getHostName())
                    .field("offset", offset)
                    .field("thread_name", event.getThreadName())
                    .field("logger", event.getLogger().getName())
                    .field("message", event.getMessage())
                    .array("tags", context.getTags())
                .endObject();
            builder.add(transport.prepareIndex(configuration.getIndex(), configuration.getType(), id)
                .setSource(contentBuilder));
            offset++;
        } catch (Exception ex) {
            System.err.println(String.format("Cannot publish event: %s", ex.getMessage()));
        }
    }

    @Override
    public void end(PublishContext context) {
        try {
            if (null != builder) {
                BulkResponse response = builder.get();
                if (response.hasFailures()) {
                    System.err.println("Elasticsearch publish failures: " + response.buildFailureMessage());
                }
            }
        } finally {
            builder = null;
            transport.close();
            transport = null;
        }
    }
}
