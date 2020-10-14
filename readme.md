# Kafka2Elastic

This is a simple example of an ETL pipeline which reads data from Kafka into ElasticSearch

# Building

This project is built with [sbt](https://www.scala-sbt.org/)

Using [kafka4m](https://github.com/aaronp/kafka4m) and its own basic Elastic REST client. 

To build a docker container:
```
sbt docker
```

To build/run a fat jar:
```
sbt assembly
```

To test w/ coverage
```
sbt clean coverage test coverageReport 
```

# Running

## To check the configuration settings, you can specify the config key like this:
```
java -jar app.jar show=app.elastic app.elastic.collection=testing123
```

which would display the config values and where they came from:
```
app.elastic.collection : testing123 # command-line
app.elastic.host :  # file:/Users/me/kafka2elastic/app/target/scala-2.13/classes/reference.conf: 9
app.elastic.masterNodeTimeout : 5s # file:/Users/me/kafka2elastic/app/target/scala-2.13/classes/reference.conf: 11
app.elastic.timeout : 10s # file:/Users/me/kafka2elastic/app/target/scala-2.13/classes/reference.conf: 13
```

To otherwise run the ETL job, you would have typically specify your own configuration in a file rather 
than <key>=<value> on the command line:

```
# myconfig.conf
app.elastic.collection :testing123
app.elastic.host : "http://my-es-host:9200"
app.kafka4m.bootstrap.servers: "kafka-broker1:9092,kafka-broker2:9092"
app.kafka4m.consumer.topic : some-topic
```
Then:
```
java -jar app.jar myconfig.conf
```



