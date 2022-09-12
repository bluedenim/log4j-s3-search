package com.van.logging.elasticsearch;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Date;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;

import org.elasticsearch.client.*;


import com.van.logging.Event;
import com.van.logging.PublishContext;
import com.van.logging.utils.StringUtils;

/**
 * Elasticsearch implementation of IPublishHelper to publish logs into Elasticsearch
 * Created by vly on 11/26/2016.
 */
public class ElasticsearchPublishHelper implements IElasticsearchPublishHelper {

    private ElasticsearchConfiguration configuration;

    private HttpHost[] httpHosts;

    // See https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/object-lifecycles.html
    // for what to clean up and how
    private ElasticsearchTransport transport;
    private BulkRequest.Builder bulkRequestBuilder;

    private int offset;
    private Date timeStamp;

    private static class Document {
        final Date timestamp;
        final String type;
        final String hostname;
        final int offset;
        final String thread_name;
        final String logger;
        final String message;
        final String[] tags;

        public Document(
            Date timestamp, String type, String hostname, int offset, String threadName, String logger,
            String message, String[] tags) {

            this.timestamp = timestamp;
            this.type = type;
            this.hostname = hostname;
            this.offset = offset;
            this.thread_name = threadName;
            this.logger = logger;
            this.message = message;
            this.tags = tags;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public String getType() {
            return type;
        }

        public String getHostname() {
            return hostname;
        }

        public int getOffset() {
            return offset;
        }

        public String getThread_name() {
            return thread_name;
        }

        public String getLogger() {
            return logger;
        }

        public String getMessage() {
            return message;
        }

        public String[] getTags() {
            return tags;
        }
    }

    public ElasticsearchPublishHelper() {
    }

    @Override
    public void initialize(ElasticsearchConfiguration configuration) {
        this.configuration = configuration;
        this.httpHosts = this.configuration.getHttpHosts().toArray(HttpHost[]::new);
    }

    @Override
    public void start(PublishContext context) {
        offset = 0;
        timeStamp = new Date();

        RestClientBuilder builder = RestClient.builder(httpHosts);
        transport = new RestClientTransport(builder.build(), new JacksonJsonpMapper());
        bulkRequestBuilder = new BulkRequest.Builder();
    }

    private Document documentForEvent(Event event, PublishContext context, int sequence) {
        return new Document(
            timeStamp, event.getType(), context.getHostName(), offset, event.getThreadName(), event.getSource(),
            event.getMessage(), context.getTags());
    }

    @Override
    public void publish(PublishContext context, int sequence, Event event) {
        try {
            String id = String.format("%s-%s-%016d", context.getCacheName(),
                context.getHostName(), offset);
            bulkRequestBuilder.operations(op -> op
                .index(idx -> idx
                    .index(configuration.getIndex())
                    .id(id)
                    .document(documentForEvent(event, context, sequence))
                )
            );
            offset++;
        } catch (Exception ex) {
            System.err.printf("Cannot publish event: %s%n", ex.getMessage());
        }
    }

    @Override
    public void end(PublishContext context) {
        try {
            if ((null != transport) && (null != bulkRequestBuilder)) {
                ElasticsearchClient client = new ElasticsearchClient(transport);
                BulkResponse response = client.bulk(bulkRequestBuilder.build());
                if (response.errors()) {
                    System.err.println("Elasticsearch publish failures: ");
                    for (BulkResponseItem item: response.items()) {
                        if (item.error() != null) {
                            System.err.println("  " + item.error().reason());
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            offset = 0;
            bulkRequestBuilder = null;
            try {
                if (null != transport) {
                    transport.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                transport = null;
            }
        }
    }

    public static IElasticsearchPublishHelper getPublishHelper(
        String publishHelperNameSpec, ClassLoader classLoader) {
        return getPublishHelper(publishHelperNameSpec, classLoader, false);
    }

    public static IElasticsearchPublishHelper getPublishHelper(
        String publishHelperNameSpec, ClassLoader classLoader, boolean verbose) {
        ElasticsearchPublishHelper helper = null;
        if (StringUtils.isTruthy(publishHelperNameSpec)) {
            System.out.printf("Attempting to instantiate %s%n", publishHelperNameSpec);
            try {
                Class<ElasticsearchPublishHelper> cls;
                cls = (Class<ElasticsearchPublishHelper>)classLoader.loadClass(
                    publishHelperNameSpec
                );
                Constructor<ElasticsearchPublishHelper> ctor = cls.getConstructor();
                helper = ctor.newInstance();
                if (verbose) {
                    System.out.printf(
                        "Successfully registered %s as Elasticsearch publish helper%n", publishHelperNameSpec
                    );
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (null == helper) {
            if (verbose) {
                System.out.printf("Instantiating the default ElasticsearchPublishHelper%n");
            }
            helper = new ElasticsearchPublishHelper();
        }
        return helper;
    }
}
