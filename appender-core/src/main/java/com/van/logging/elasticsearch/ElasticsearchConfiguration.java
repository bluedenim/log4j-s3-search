package com.van.logging.elasticsearch;

import com.van.logging.utils.StringUtils;
import org.apache.http.HttpHost;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Configuration for Elasticsearch publisher
 *
 * Created by vly on 11/26/2016.
 */
public class ElasticsearchConfiguration {
    private static final String DEFAULT_CLUSTERNAME = "elasticsearch";
    private static final String DEFAULT_INDEX = "logindex";

    // Elasticsearch is desperately trying to get rid of type, but this is here just in case.
    private static final String DEFAULT_TYPE = "_doc";


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

    public Stream<HttpHost> getHttpHosts() {
        return hosts.stream().map((String hostString) -> {
            int port = 9300;
            String hostName = null;
            String scheme = null;

            try {
                if (StringUtils.isTruthy(hostString)) {
                    if (hostString.startsWith("http://") || hostString.startsWith("https://")) {
                        // The newer format of "scheme://host:port"
                        URL parsedUrl = new URL(hostString);
                        hostName = parsedUrl.getHost();
                        port = parsedUrl.getPort();
                        if (port < 0) {
                            port = 9300;
                        }
                        scheme = parsedUrl.getProtocol();
                    } else {
                        // The older format of just "host:port"
                        String parts[] = hostString.split(":");
                        hostName = parts[0].trim();
                        try {
                            port = Integer.parseInt(parts[1]);
                        } catch (Exception ex) {
                        }
                    }
                    return new HttpHost(hostName, port, scheme);
                }
            } catch(Exception ex) {
                System.err.println(
                    String.format("Cannot parse host %s: %s", hostString, ex)
                );
            }
            return null;
        }).filter(Objects::nonNull);
    }

    @Override
    public String toString() {
        return String.format(
            "ElasticsearchConfiguration: cluster=%s, hosts=%s, index=%s, type=%s",
            clusterName, hosts.toString(), index, type);
    }
}
