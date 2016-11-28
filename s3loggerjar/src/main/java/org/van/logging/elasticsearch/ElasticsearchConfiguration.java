package org.van.logging.elasticsearch;

import java.util.LinkedList;
import java.util.List;

/**
 * Configuration for Elasticsearch publisher
 *
 * Created by vly on 11/26/2016.
 */
public class ElasticsearchConfiguration {
    public interface IHostConsumer {
        void consume(String host, int port);
    }

    private static final String DEFAULT_CLUSTERNAME = "elasticsearch";
    private static final String DEFAULT_INDEX = "logindex";
    private static final String DEFAULT_TYPE = "log";


    private String clusterName = DEFAULT_CLUSTERNAME;
    private String index = DEFAULT_INDEX;
    private String type = DEFAULT_TYPE;

    private final List<String> hosts = new LinkedList<>();




    public ElasticsearchConfiguration() {
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Adds a host to the list of hosts to use to contact ElasticSearch.
     *
     * @param host the host and port (e.g. "host1:9300" or "host1")
     */
    public void addHost(String host) {
        hosts.add(host);
    }

    /**
     * Iterate through all the host:port entries and call the supplied consumer with each.
     *
     * @param consumer a consumer of host and port for an Elasticsearch host entry
     */
    public void iterateHosts(IHostConsumer consumer) {
        for (String host: hosts) {
            String parts[] = host.split(":");
            int port = 9300;
            String hostName = parts[0].trim();
            try {
                port = Integer.parseInt(parts[1]);
            } catch (Exception ex) {
            }
            consumer.consume(hostName, port);
        }
    }
}
