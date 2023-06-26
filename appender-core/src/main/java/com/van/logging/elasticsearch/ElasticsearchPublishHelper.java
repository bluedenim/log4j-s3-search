package com.van.logging.elasticsearch;

import com.van.logging.PublishContext;
import com.van.logging.VansLogger;
import com.van.logging.utils.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import com.van.logging.Event;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Date;

import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;

/**
 * Elasticsearch implementation of IPublishHelper to publish logs into Elasticsearch
 * Created by vly on 11/26/2016.
 */
public class ElasticsearchPublishHelper implements IElasticsearchPublishHelper {

    private ElasticsearchConfiguration configuration;

    private HttpHost[] httpHosts;
    private RestHighLevelClient client;
    private BulkRequest bulkRequest;
    private int offset;
    private Date timeStamp;

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
        client = new RestHighLevelClient(builder);

        bulkRequest = new BulkRequest();
    }

    @Override
    public void publish(PublishContext context, int sequence, Event event) {
        try {
            String id = String.format("%s-%s-%016d", context.getCacheName(),
                context.getHostName(), offset);
            XContentBuilder contentBuilder = jsonBuilder()
                .startObject()
                .field("timestamp", timeStamp)
                .field("type", event.getType())
                .field("hostname", context.getHostName())
                .field("offset", offset)
                .field("thread_name", event.getThreadName())
                .field("logger", event.getSource())
                .field("message", event.getMessage())
                .array("tags", context.getTags())
                .endObject();
            bulkRequest.add(new IndexRequest(configuration.getIndex()).id(id).source(contentBuilder));
            offset++;
        } catch (Exception ex) {
            VansLogger.logger.error("Cannot publish event", ex);
        }
    }

    @Override
    public void end(PublishContext context) {
        try {
            if ((null != client) && (null != bulkRequest)) {
                BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                if (response.hasFailures()) {
                    VansLogger.logger.error("Elasticsearch publish failures: " + response.buildFailureMessage());
                }
            }
        } catch (IOException ex) {
            VansLogger.logger.error("Cannot end publish batch", ex);
        } finally {
            try {
                if (null != client) {
                    client.close();
                }
                bulkRequest = null;
            } catch (IOException e) {
                VansLogger.logger.error("Error closing client", e);
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
            VansLogger.logger.info(String.format("Instantiating %s", publishHelperNameSpec));
            try {
                Class<ElasticsearchPublishHelper> cls;
                cls = (Class<ElasticsearchPublishHelper>)classLoader.loadClass(
                    publishHelperNameSpec
                );
                Constructor<ElasticsearchPublishHelper> ctor = cls.getConstructor();
                helper = ctor.newInstance();
                if (verbose) {
                    VansLogger.logger.info(
                        String.format(
                            "Successfully registered %s as Elasticsearch publish helper", publishHelperNameSpec
                        )
                    );
                }
            } catch (Exception ex) {
                VansLogger.logger.error(String.format("Cannot set up %s", publishHelperNameSpec), ex);
            }
        }

        if (null == helper) {
            if (verbose) {
                VansLogger.logger.info("Instantiating the default ElasticsearchPublishHelper");
            }
            helper = new ElasticsearchPublishHelper();
        }
        return helper;
    }
}
