package com.van.logging.elasticsearch;

import com.van.logging.IPublishHelper;
import com.van.logging.PublishContext;
import com.van.logging.utils.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.common.xcontent.XContentBuilder;
import com.van.logging.Event;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Elasticsearch implementation of IPublishHelper to publish logs into Elasticsearch
 * Created by vly on 11/26/2016.
 */
public class ElasticsearchPublishHelper implements IPublishHelper<Event> {

    private final ElasticsearchConfiguration configuration;

    private final List<Node> nodes = new ArrayList<>();
    private RestHighLevelClient client;
    private BulkRequest bulkRequest;
    private int offset;
    private Date timeStamp;

    public ElasticsearchPublishHelper(ElasticsearchConfiguration configuration) {
        this.configuration = configuration;
        this.configuration.iterateHosts((host, port) -> {
            nodes.add(createNodeFromHost(host, port));
        });
    }

    Node createNodeFromHost(String host, int port) {
        String scheme = null;
        String hostName = host.toLowerCase().trim();
        if (hostName.startsWith("http:") || hostName.startsWith("https:")) {
            String[] schemeAndHostname = hostName.split(":");
            if (schemeAndHostname.length >= 2) {
                scheme = schemeAndHostname[0];
                hostName = schemeAndHostname[1];
            }
        }
        return new Node(new HttpHost(hostName, port, scheme));
    }

    @Override
    public void start(PublishContext context) {
        offset = 0;
        timeStamp = new Date();

        // The way RestClient.builder(Node... hosts) is designed requires hard-coding the list of hosts (great
        // for demo programs but not for anything more serious).
        // Because our host list is materialized from configuration as an array/list, we have to actually call
        // RestClient.builder(Node... hosts) with an array via inflection. All this BS would not be necessary
        // if they had just added a RestClient.builder(Node[] nodes) or RestClient.builder(Collection<Node> nodes).
        try {
            Method restClientBuilderMethod = RestClient.class.getDeclaredMethod("builder", Node[].class);
            restClientBuilderMethod.setAccessible(true);
            RestClientBuilder builder =
                (RestClientBuilder)restClientBuilderMethod.invoke(null, new Object[]{nodes.toArray(new Node[0])});
            client = new RestHighLevelClient(builder);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            System.err.printf("Cannot publish event: %s%n", ex.getMessage());
        }
    }

    @Override
    public void end(PublishContext context) {
        try {
            if ((null != client) && (null != bulkRequest)) {
                BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                if (response.hasFailures()) {
                    System.err.println("Elasticsearch publish failures: " + response.buildFailureMessage());
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (null != client) {
                    client.close();
                }
                bulkRequest = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static ElasticsearchPublishHelper getPublishHelper(
        String publishHelperNameSpec, ElasticsearchConfiguration elasticsearchConfiguration, ClassLoader classLoader) {
        ElasticsearchPublishHelper helper = null;
        if (StringUtils.isTruthy(publishHelperNameSpec)) {
            System.out.printf("Attempting to instantiate %s%n", publishHelperNameSpec);
            try {
                Class<ElasticsearchPublishHelper> cls;
                cls = (Class<ElasticsearchPublishHelper>)classLoader.loadClass(
                    publishHelperNameSpec
                );
                Constructor<ElasticsearchPublishHelper> ctor = cls.getConstructor(ElasticsearchConfiguration.class);
                helper = ctor.newInstance(elasticsearchConfiguration);
                System.out.printf("Successfully registered %s as Elasticsearch publish helper%n", publishHelperNameSpec);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (null == helper) {
            System.out.printf("Instantiating the default ElasticsearchPublishHelper%n");
            helper = new ElasticsearchPublishHelper(elasticsearchConfiguration);
        }
        return helper;
    }
}
