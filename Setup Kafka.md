This is to help with Local testing

Download Kafka: Go to the Apache Kafka download page and download the latest binary (.tgz).

Extract the Archive:

```
tar -xzf kafka_2.13-<version>.tgz
cd kafka_2.13-<version>
export KAFKADIR=`pwd`
```

Start Zookeeper: Kafka requires Zookeeper, which is bundled with Kafka.

```
cd $KAFKADIR
bin/zookeeper-server-start.sh config/zookeeper.properties
```

This will start a Zookeeper service required for Kafka.

Start Kafka Broker:

Open a new terminal window and navigate to the Kafka directory again:

```
cd $KAFKADIR
bin/kafka-server-start.sh config/server.properties
```

Create a Topic:

Open another terminal window to create a new topic:

```
cd $KAFKADIR
bin/kafka-topics.sh --create --topic fixdata --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1
```

Send Messages:

In another terminal window, start a producer to send messages to your topic:

Download and build  [https://github.com/johnlpage/FixMaker] to generate FixMessages

```shell
java -jar target/FixMaker-1.0-SNAPSHOT.jar 1000000 fix.json


kafka-console-producer.sh --topic fixdata --bootstrap-server localhost:9092 < fix.json | wc -c
```
