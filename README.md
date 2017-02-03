# log4j-s3-search 

(See [therealvan.com/s3loggerappender.html](http://www.therealvan.com/s3loggerappender.html) for write-up page.)

A [Log4j appender](http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Appender.html) implementation that will collect log events into a staging buffer up to a configured size to then publish to external store such as:
*  [AWS S3](http://aws.amazon.com/s3/) for remote storage/archive.
*  [Apache Solr](http://lucene.apache.org/solr/) for search.
*  [Elasticsearch](https://www.elastic.co/guide/index.html) for search.

**All external stores** above are optional (although to be of any use at least one should be used).  If no configuration is found for S3, for instance, the appender will not attempt to publish to S3.  Likewise, if there is not configuration for Apache Solr, the appender will not attempt to publish to Solr.

## Installation
Download the code and build the .jar to include in your program.  The code is 100% Java, so building the jar will be a breeze.  You will need the basics:
* [JSDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Maven](http://maven.apache.org/)

```
mvn clean package 
```

### Running the sample
To run the sample program **s3loggersample**, some additional steps:

```
cd s3loggersample
mvn assembly:assembly
java -cp target\s3loggersample-0.0.4-jar-with-dependencies.jar org.van.example.Main
```

## Usage
The `s3loggerjar-x.y.z.jar` is the only JAR/dependency you need for your program. You can see how a sample program can be set up by looking at how **s3loggersample** uses the jar:

* You will need to either build or download a prebuilt `s3loggerjar-x.y.z.jar` (e.g. from the `dist/` subdir) and add that to your program's dependency.
* You will need to add (create if necessary) a `log4j.properties` file for you program that configures the logger appender. Again, you can see how it is done in the **s3loggersample** module.


## Configuration
### General
In addition to the typical appender configuration (such as layout, Threshold, etc.), these common properties control the appender in general:
*  **stagingBufferSize** -- the number of entries to collect for a batch before publishing (default is 2000).
*  **stagingBufferAge** -- (optional) if specified, the number of *minutes* to wait before publishing a batch. If used,
         this parameter will override the condition set by *stagingBufferSize*. The value must be >= 1.
*  **tags** -- comma-separated tokens to associate to the log entries (used mainly for search filtering). Examples:
    *  `production,webserver`
    *  `qa,database`

A sample snippet from `log4j.properties` to publish whenever 2500 events are collected:
```
log4j.appender.S3Appender=org.van.logging.log4j.S3LogAppender
log4j.appender.S3Appender.layout=org.apache.log4j.PatternLayout
log4j.appender.S3Appender.layout.conversionPattern=%d %p [%t] %c %m
log4j.appender.S3Appender.Threshold=WARN

log4j.appender.S3Appender.tags=TEST,ONE,TWO
log4j.appender.S3Appender.stagingBufferSize=2500
```

or, if a time-based publishing policy is desired (e.g. publish every 15 minutes):
```
log4j.appender.S3Appender=org.van.logging.log4j.S3LogAppender
log4j.appender.S3Appender.layout=org.apache.log4j.PatternLayout
log4j.appender.S3Appender.layout.conversionPattern=%d %p [%t] %c %m
log4j.appender.S3Appender.Threshold=WARN

log4j.appender.S3Appender.tags=TEST,ONE,TWO
log4j.appender.S3Appender.stagingBufferAge=15
```

### S3
These properties (**please use your own values**) control how the logs will be stored in S3:
* **s3Region** -- the AWS region to use (e.g. "us-west-2").
* **s3Bucket** -- the S3 bucket to use.  The logger will attempt to create this bucket if it doesn't already exist.
* **s3Path** -- the path to the uploaded files (key prefix under the hood)

AWS credentials are required to interact with S3.  The recommended way is using either 1) instance profiles (when working with EC2 instances) or 2) creating `%USERPROFILE%\.aws\credentials` (Windows) or `~/.aws/credentials`.

These properties can also be overridden in Log4j configuration for `S3LogAppender`:
* **s3AccessKey** and **s3SecretKey** -- access and secret keys.  
When these properties are present in the configuration, they *take precedence over* the default sources in the credential chain as described earlier.

A sample snippet from `log4j.properties` (with the optional s3AccessKey and s3SecretKey properties set):
```
log4j.appender.S3Appender.s3Region=us-west-2
log4j.appender.S3Appender.s3Bucket=acmecorp
log4j.appender.S3Appender.s3Path=logs/myApplication/

# Optional access and secret keys
log4j.appender.S3Appender.s3AccessKey=CMSADEFHASFHEUCBEOERUE
log4j.appender.S3Appender.s3SecretKey=ASCNEJAERKE/SDJFHESNCFSKERTFSDFJESF
```

The final S3 key used in the bucket follows the format:
```
{s3Path}/yyyyMMddHH24mmss_{UUID w/ "-" stripped}

e.g.

logs/myApplication/20150327081000_localhost_6187f4043f2449ccb4cbd3a7930d1130
```


### Solr
There is only one property for Solr: the REST endpoint to the core/collection:
* **solrUrl** -- the URL to core/collection

A sample snippet from `log4j.properties`:
```
log4j.appender.S3Appender.solrUrl=http://localhost:8983/solr/log-events/
```

### Elasticsearch
There are four properties for Elasticsearch, all but one are optional:
* **elasticsearchCluster** -- the cluster name (default if "elasticsearch")
* **elasticsearchIndex** -- the index in which to store the log data (default is "logindex")
* **elasticsearchType** -- the type of a log data entry (default is "log")
* **elasticsearchHosts** -- comma-delimited list of `host:port` values. There is no default; this property is *required*. 

```
log4j.appender.S3Appender.elasticsearchCluster=elasticsearch
log4j.appender.S3Appender.elasticsearchIndex=logindex
log4j.appender.S3Appender.elasticsearchType=log
log4j.appender.S3Appender.elasticsearchHosts=localhost:9300
```

## Solr Integration
A new core should be created for the log events.  The setting up of Apache Solr and the setting up of a core are outside the scope of this file.  However, a sample template for a `schema.xml` that can be used is included in this repo as `/misc/solr/schema.xml`.

Each log event will be indexed as a Solr document.  The "id" property for each document 
will follow the format:
```
yyyyMMddHH24mmss_{host name}_{UUID w/ "-" stripped}-{host name}-{sequence}

e.g.

20150327081000_mycomputer_6187f4043f2449ccb4cbd3a7930d1130-mycomputer-0000000000000012
```

*NOTE* that this ID is formatted such that one can cross-reference a 
document to the S3 batch from which the corresponding log event can be found.

```
String id = solrDoc.getFieldValue("id").toString();
String s3Key = id.substring(0, id.indexOf("-"));
```

## Elasticsearch Integration
A new index should be created for the log events.  The setting up of Elasticsearch and the index are outside the scope of this file.  However, a sample template for the index schema that can be used is included in this repo as `/misc/elasticsearch/logindex.json`.
This schema should be installed before any log entries are added. A typical PUT to `/<elasticsearch host>:9200/<index>` with
the body of the JSON should be sufficient. 

Each log event will be indexed as a document.  The "id" property for each document 
will follow the format:
```
yyyyMMddHH24mmss_{host name}_{UUID w/ "-" stripped}-{host name}-{sequence}

e.g.

20150327081000_mycomputer_6187f4043f2449ccb4cbd3a7930d1130-mycomputer-0000000000000012
```

*NOTE* that this ID is formatted such that one can cross-reference a 
document to the S3 batch from which the corresponding log event can be found.

```
String id = solrDoc.getFieldValue("id").toString();
String s3Key = id.substring(0, id.indexOf("-"));
```
