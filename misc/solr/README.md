### Solr Test Configuration

This subdirectory has configuration files set up so that it can
be mapped to a volume at `/var/solr` to get the Docker instance
of Solr to use.

To use this:
* Make sure **docker-compose** is at version **1.5+**
* Set the environment variable `SOLR_CORE_DIR` to this directory
  on the file system (e.g. `~/proj/log4j-s3-search/misc/solr`)
* Start the Solr container:
  ```
  docker-compose up solr
  ```
* The Solr server will be accessible at http://localhost:8983/solr/#/
* You can take a look at https://github.com/bluedenim/log4j-s3-search-samples/blob/master/appender-log4j2-sample/src/main/resources/log4j2.xml#L55-L56
  to see how to configure a program to send logs to it.
