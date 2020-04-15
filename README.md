# log4j-s3-search 

A [Log4j appender](http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Appender.html) implementation that 
will collect log events into a staging buffer up to a configured size to then publish to external stores such as:

*  [AWS S3](http://aws.amazon.com/s3/) for remote storage/archive.
*  [Azure Blob Storage](https://azure.microsoft.com/en-us/services/storage/blobs/) for remote storage/archive.
*  [Google Cloud Storage](https://cloud.google.com/storage) for remote storage/archive.
*  [Apache Solr](http://lucene.apache.org/solr/) for search.
*  [Elasticsearch](https://www.elastic.co/guide/index.html) for search.

**All external stores** above are optional (although to be of any use at least one should be used).  If no 
configuration is found for S3, for instance, the appender will not attempt to publish to S3.  Likewise, if there 
is not configuration for Apache Solr, the appender will not attempt to publish to Solr.


## Packages
The project is broken up into several packages:

* **appender-core** -- Log4j version-agnostic logic that deals with maintaining the log staging buffer and publishing to external stores.
* **appender-log4j** -- Log4j 1.x binding code that, together with **appender-core**, will allow client code to use the project with Log4j 1.x.
* **appender-log4j2** -- Log4j 2.x binding code that, together with **appender-core**, will allow client code to use the project with Log4j 2.x.

* **appender-log4j-sample** -- a sample client illustrating how to use the project with Log4j 1.x.
* **appender-log4j2-sample** -- a sample client illustrating how to use the project with Log4j 2.x.


## Usage
* Find out which version of Log4j your client program is using.
  * If you're using **Log4j 1.x**, you should add **appender-core** and **appender-log4j** as dependencies. (See **appender-log4j-sample** for an example of how it's done.)
  * If you're using **Log4j 2.x**, you should add **appender-core** and **appender-log4j2** as dependencies. (See **appender-log4j2-sample** for an example of how it's done.)

### Maven Dependencies
 
For example, when coding with Log4j 1.x:

Please **substitute in the latest version** in your case (so I don't have to keep updating this README.md).

```
<dependency>
    <groupId>com.therealvan</groupId>
    <artifactId>appender-core</artifactId>
    <version>2.3.1</version>
</dependency>
<dependency>
    <groupId>com.therealvan</groupId>
    <artifactId>appender-log4j</artifactId>
    <version>2.3.1</version>
</dependency>
```

Similarly, when coding with Log4j 2.x:
```
<dependency>
    <groupId>com.therealvan</groupId>
    <artifactId>appender-core</artifactId>
    <version>2.3.1</version>
</dependency>
<dependency>
    <groupId>com.therealvan</groupId>
    <artifactId>appender-log4j2</artifactId>
    <version>2.3.1</version>
</dependency>
```

### Obsolete versions

_Please ignore the non-semver versions **2.0** and **0.3.0**_.



## Running the sample programs

NOTE that the sample programs set up a very small batch size (publish every 10 entries) for illustration purposes. 
In your programs you will most likely use a much higher number for the **stagingBufferSize** property.

### Log4j Example
To run the sample program **appender-log4j-sample**:

```
cd appender-log4j-sample
```
Modify `src\main\resources\log4j.properties` to use _your storage configuration_.
```
mvn clean install
mvn assembly:assembly
java -cp target\log4j-s3-search-log4j-sample-jar-with-dependencies.jar com.van.example.Main
```

### Log4j 2.x Example
To run the sample program **appender-log4j2-sample**:

```
cd appender-log4j2-sample
```
Modify `src\main\resources\log4j2.xml` to use _your storage configuration_.
```
mvn clean install
java -cp target\log4j-s3-search-log4j2-sample.jar com.van.example.Main
```

_There is currently some complication w/ Log4j 2 such that the packaging is done differently than 
that for the Log4j 1.x example. The method used is documented [here.](https://stackoverflow.com/questions/34945438/log4j2-configuration-not-found-when-running-standalone-application-builded-by-sh/34946780)_

### Log4j 1.x Notes
There is currently a security vulnerability with Log4j 1.x (https://github.com/advisories/GHSA-2qrg-x229-3v8q). 

In addition, Log4j hasn't been worked on since mid-2012. All activities have gone into Log4j 2.x.


## Configuration
### General
In addition to the typical appender configuration (such as layout, Threshold, etc.), these common properties control the appender in general:
*  **stagingBufferSize** -- the number of entries to collect for a batch before publishing (default is 2000).
*  **stagingBufferAge** -- (optional) if specified, the number of *minutes* to wait before publishing a batch. If used,
         this parameter will override the condition set by *stagingBufferSize*. The value must be >= 1.
*  **tags** -- comma-separated tokens to associate to the log entries (used mainly for search filtering). Examples:
    *  `production,webserver`
    *  `qa,database`

_All the examples here are using the log4j.properties format for Log4j 1.x. See the module **appender-log4j2-sample** to find out how to do it for Log4j 2.x._

A sample snippet from `log4j.properties` to publish whenever 2500 events are collected:
```
log4j.appender.L4jAppender=com.van.logging.log4j.Log4jAppender
log4j.appender.L4jAppender.layout=org.apache.log4j.PatternLayout
log4j.appender.L4jAppender.layout.conversionPattern=%d %p [%t] %c %m
log4j.appender.L4jAppender.Threshold=WARN

log4j.appender.L4jAppender.tags=TEST,ONE,TWO
log4j.appender.L4jAppender.stagingBufferSize=2500
```

or, if a time-based publishing policy is desired (e.g. publish every 15 minutes):
```
log4j.appender.L4jAppender=com.van.logging.log4j.Log4jAppender
log4j.appender.L4jAppender.layout=org.apache.log4j.PatternLayout
log4j.appender.L4jAppender.layout.conversionPattern=%d %p [%t] %c %m
log4j.appender.L4jAppender.Threshold=WARN

log4j.appender.L4jAppender.tags=TEST,ONE,TWO
log4j.appender.L4jAppender.stagingBufferAge=15
```

### S3
These properties (**please use your own values**) control how the logs will be stored in S3:
* **s3Bucket** -- the S3 bucket to use.  The logger will attempt to create this bucket if it doesn't already exist.
* **s3Path** -- the path to the uploaded files (S3 key prefix under the hood)

These properties determine how to connect to S3:
* **s3Region** -- the AWS region to use (e.g. "us-west-2").
* **s3ServiceEndpoint** -- the service endpoint to use instead of the default.
* **s3SigningRegion** -- the region to use for signing requests.

Use either:
  - **s3Region** or
  - **s3ServiceEndpoint** and **s3SigningRegion**

but not all three simultaneously. You will get an error from AWS if you use all three.

AWS credentials are required to interact with S3.  **NOTE** that the recommended way of configuring
the credentials is:
  1) using roles assigned to instance profiles (when working with EC2 instances) or 
  2) creating a credentials file on the computer running the program as 
  `%USERPROFILE%\.aws\credentials` (Windows) or `~/.aws/credentials` (see https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/credentials.html#credentials-file-format)

If the above methods are not possible for your situation, these properties can also be overridden in 
the optional Log4j configuration:
* **s3AwsKey** and **s3AwsSecret** -- access and secret keys.  
* **s3AwsSessionToken** -- session token for short-lived credentials.

When these properties are present in the configuration, they *take precedence over* the default sources in the credential chain as described earlier.

A sample snippet from `log4j.properties` (with the optional s3AwsKey and s3AwsSecret properties set):
```
log4j.appender.L4jAppender.s3Region=us-west-2
log4j.appender.L4jAppender.s3Bucket=acmecorp
log4j.appender.L4jAppender.s3Path=logs/myApplication/

# Optional access and secret keys
log4j.appender.L4jAppender.s3AwsKey=CMSADEFHASFHEUCBEOERUE
log4j.appender.L4jAppender.s3AwsSecret=ASCNEJAERKE/SDJFHESNCFSKERTFSDFJESF
```

The final S3 key used in the bucket follows the format:
```
{s3Path}/yyyyMMddHH24mmss_{hostname}_{UUID w/ "-" stripped}

e.g.

logs/myApplication/20150327081000_localhost_6187f4043f2449ccb4cbd3a7930d1130
```

Content configurations
* **s3Compression** -- if set to "true," then contents will be GZIP'ed before publishing into S3
* **s3SseKeyType** -- if set to "SSE_S3," then contents published will be flagged to use SSE-S3 encryption (see https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingServerSideEncryption.html)

### Azure Blob
These properties (**please use your own values**) control how the logs will be stored in Azure Blob Storage:
* **azureBlobContainer** -- the storage container name.
* **azureBlobNamePrefix** -- the prefix for the blob name.
* **azureBlobCompressionEnabled** -- if set to "true," then contents will be GZIP'ed before publishing.
* **azureStorageConnectionString** -- optional value for the connection string for connecting to Azure. See note below.

A sample snippet from `log4j.properties` (with the optional azureStorageConnectionString property set):
```
log4j.appender.L4jAppender.azureBlobContainer=my-container
log4j.appender.L4jAppender.azureBlobNamePrefix=logs/myApplication/

# Optional
log4j.appender.L4jAppender.azureBlobCompressionEnabled=false
log4j.appender.L4jAppender.azureStorageConnectionString=DefaultEndpointsProtocol=https;AccountName=...;EndpointSuffix=core.windows.net
```

Just as the case of S3, the final blob name used in the container follows the format:
```
{azureBlobNamePrefix}/yyyyMMddHH24mmss_{hostname}_{UUID w/ "-" stripped}

e.g.

logs/myApplication/20150327081000_localhost_6187f4043f2449ccb4cbd3a7930d1130
```

Notes:
* See https://docs.microsoft.com/en-us/rest/api/storageservices/Naming-and-Referencing-Containers--Blobs--and-Metadata for rules on names.
* From various examples online, the preferred way to establish the Azure connection string is to set the environment
  variable `AZURE_STORAGE_CONNECTION_STRING` on the hosts running your code. 
  However, you can also set the `azureStorageConnectionString` property for local testing.
  
See [Azure Storage connection strings](https://docs.microsoft.com/en-us/azure/storage/common/storage-configure-connection-string) for more info on
connection strings.


### Google Cloud Storage
These properties (**please use your own values**) control how the logs will be stored in GCP Storage service:
* **gcpStorageBucket** -- the storage bucket name.
* **gcpStorageBlobNamePrefix** -- the prefix for the blob name.
* **gcpStorageCompressionEnabled** -- if set to "true," then contents will be GZIP'ed before publishing. 
  The default is "false."

Just as in the case with AWS S3, there is an [extensive authentication process](https://github.com/googleapis/google-cloud-java#authentication) and list of options.
This tool will assume the running process has the necessary authentication setup done.

While working on this, for example, I downloaded my service account's JSON key file and set the environment 
variable `GOOGLE_APPLICATION_CREDENTIALS` to the full path to the file. This allowed my programs using the
[Store API](https://cloud.google.com/storage/docs/reference/libraries#client-libraries-install-java)
to work without doing any specific authentication calls.

A sample snippet from `log4j.properties`:
```
log4j.appender.L4jAppender.gcpStorageBucket=my-bucket
log4j.appender.L4jAppender.gcpStorageBlobNamePrefix=logs/myApplication/

# Optional
log4j.appender.L4jAppender.gcpStorageCompressionEnabled=false
```

Just as the other cases, the final blob name used in the bucket follows the format:
```
{gcpStorageBlobNamePrefix}/yyyyMMddHH24mmss_{hostname}_{UUID w/ "-" stripped}

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
log4j.appender.L4jAppender.elasticsearchCluster=elasticsearch
log4j.appender.L4jAppender.elasticsearchIndex=logindex
log4j.appender.L4jAppender.elasticsearchType=log
log4j.appender.L4jAppender.elasticsearchHosts=localhost:9300
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
