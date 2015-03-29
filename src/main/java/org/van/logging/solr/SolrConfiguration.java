package org.van.logging.solr;

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
		if ((null != solrUrl) && (solrUrl.length() > 0)) {
			try {
				this.url = new URL(solrUrl);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}
	
}
