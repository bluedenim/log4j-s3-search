package com.van.logging.solr;

import com.van.logging.VansLogger;
import com.van.logging.utils.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Solr configuration
 *
 * @author vly
 *
 */
public class SolrConfiguration {
    private URL url;

    public URL getUrl() {
        return url;
    }

    public void setUrl(String solrUrl) {
        if (StringUtils.isTruthy(solrUrl)) {
            try {
                this.url = new URL(solrUrl);
            } catch (MalformedURLException e) {
                VansLogger.logger.error(String.format("Cannot set Solr URL to %s", solrUrl), e);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("SolrConfiguration: %s", url);
    }
}