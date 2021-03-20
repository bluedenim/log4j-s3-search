package com.van.logging.elasticsearch;


import com.van.logging.Event;
import com.van.logging.IPublishHelper;


/**
 * Interface for implementations of an IPublishHelper to publish events to Elasticsearch.
 */
public interface IElasticsearchPublishHelper extends IPublishHelper<Event> {

    /**
     * Initialize the publish helper immediately after construction
     *
     * @param configuration configuration information to initialize the publish helper with
     */
    public void initialize(ElasticsearchConfiguration configuration);
}
