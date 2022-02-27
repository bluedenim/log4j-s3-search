package com.van.logging.elasticsearch;

import org.apache.http.HttpHost;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class ElasticsearchConfigurationTest {
    private ElasticsearchConfiguration instance;

    @Before
    public void setUp() {
        instance = new ElasticsearchConfiguration();
    }

    @Test
    public void testLegacyValues() {
        instance.addHost("localhost:9300");
        instance.addHost("another:9400");
        instance.addHost("yetanother");

        HttpHost[] hosts = instance.getHttpHosts().toArray(HttpHost[]::new);
        assertEquals(3, hosts.length);

        assertEquals("http", hosts[0].getSchemeName());
        assertEquals("localhost", hosts[0].getHostName());
        assertEquals(9300, hosts[0].getPort());

        assertEquals("http", hosts[1].getSchemeName());
        assertEquals("another", hosts[1].getHostName());
        assertEquals(9400, hosts[1].getPort());

        assertEquals("http", hosts[2].getSchemeName());
        assertEquals("yetanother", hosts[2].getHostName());
        assertEquals(9300, hosts[2].getPort());
    }

    @Test
    public void testHostsWithHostNames() {
        instance.addHost("http://localhost");
        instance.addHost("https://localhost");

        HttpHost[] hosts = instance.getHttpHosts().toArray(HttpHost[]::new);

        assertEquals(2, hosts.length);
        assertEquals("http", hosts[0].getSchemeName());
        assertEquals("localhost", hosts[0].getHostName());
        assertEquals(9300, hosts[0].getPort());

        assertEquals("https", hosts[1].getSchemeName());
        assertEquals("localhost", hosts[1].getHostName());
        assertEquals(9300, hosts[1].getPort());
    }

    @Test
    public void testHostsWithHostNamesAndPort() {
        instance.addHost("http://localhost:1234");
        instance.addHost("https://localhost:5678");

        HttpHost[] hosts = instance.getHttpHosts().toArray(HttpHost[]::new);

        assertEquals(2, hosts.length);
        assertEquals("http", hosts[0].getSchemeName());
        assertEquals("localhost", hosts[0].getHostName());
        assertEquals(1234, hosts[0].getPort());

        assertEquals("https", hosts[1].getSchemeName());
        assertEquals("localhost", hosts[1].getHostName());
        assertEquals(5678, hosts[1].getPort());
    }

    @Test
    public void testSkipBadValues() {
        instance.addHost("http://localhost:9300");
        instance.addHost("bad:invalidport");
        instance.addHost("https://yetanother");
        instance.addHost("http://");
        instance.addHost("");
        instance.addHost("  \n \t");

        HttpHost[] hosts = instance.getHttpHosts().toArray(HttpHost[]::new);

        assertEquals(3, hosts.length);
        assertEquals("http", hosts[0].getSchemeName());
        assertEquals("localhost", hosts[0].getHostName());
        assertEquals(9300, hosts[0].getPort());

        assertEquals("http", hosts[1].getSchemeName());
        assertEquals("bad", hosts[1].getHostName());
        assertEquals(9300, hosts[1].getPort());

        assertEquals("https", hosts[2].getSchemeName());
        assertEquals("yetanother", hosts[2].getHostName());
        assertEquals(9300, hosts[2].getPort());
    }
}
