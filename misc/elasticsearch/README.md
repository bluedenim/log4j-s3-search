### Elasticsearch Test Configuration

This subdirectory has a `logindex.json` that can be used to 
initialize an index on an Elasticsearch server.

To use the test configuration in this repo:

* Bring up the server:
  ```
  docker-compose up elasticsearch
  ```
* Ensure you get a response from http://localhost:9200/
* Use `curl` or similar tool to create the index **log4js3** with the file `logindex.json`. E.g.
  ```
  curl -XPUT --header "Content-Type:application/json" -d "@logindex.json" http://localhost:9200/log4js3
  ```
  
  You may use a different index name than "log4js3," of course.
* Verify index is created successfully by checking http://localhost:9200/log4js3/_search
* You can now send logs to the index hosted on the local server at http://localhost:9200.
* You can take a look at https://github.com/bluedenim/log4j-s3-search-samples/blob/master/appender-log4j2-sample/src/main/resources/log4j2.xml#L59-L64 
  to see how to configure a program to send logs to it.
