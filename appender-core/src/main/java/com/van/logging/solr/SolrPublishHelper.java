package com.van.logging.solr;

import com.van.logging.Event;
import com.van.logging.IPublishHelper;
import com.van.logging.PublishContext;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * Publish helper to publish logs to Solr
 *
 * @author vly
 *
 */
public class SolrPublishHelper implements IPublishHelper<Event> {
    private final SolrClient client;

    private int offset;
    private Date timeStamp;
    private List<SolrInputDocument> docs;

    public SolrPublishHelper(URL collectionUrl) {
        this.client = new HttpSolrClient.Builder()
            .withBaseSolrUrl(collectionUrl.toExternalForm())
            .build();
    }

    public void publish(PublishContext context, int sequence, Event event) {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", String.format("%s-%s-%016d", context.getCacheName(),
            context.getHostName(), offset));
        doc.addField("timestamp", timeStamp);
        doc.addField("type", event.getType());
        doc.addField("hostname", context.getHostName());
        doc.addField("offset", offset);
        doc.addField("thread_name", event.getThreadName());
        doc.addField("logger", event.getSource());
        doc.addField("message", event.getMessage());
        if (context.getTags().length > 0) {
            for (String tag: context.getTags()) {
                doc.addField("tags", tag);
            }
        }
        docs.add(doc);
        offset++;
    }

    public void start(PublishContext context) {
        offset = 0;
        timeStamp = new Date();
        docs = new LinkedList<>();
    }

    public void end(PublishContext context) {
        try {
            client.add(docs);
            client.commit();
        } catch (Exception e) {
            throw new RuntimeException("Cannot commit batch", e);
        }
    }

}
