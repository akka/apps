# Distributed Pub Sub Benchmark

Sample usage
```bash
curl -X POST "http://localhost:8080/bench?numberOfNodes=1&messagesPerPublisher=10&numberOfTopics=200&numberOfPublishers=10&numberOfSubscribers=10"
```

To list the results of completed runs:
```bash
curl http://localhost:8080/bench
```

Run additional nodes locally with:
```
sbt -J-Dakka.cluster.roles.0=banana -J-Dakka.remote.artery.canonical.port=0
pubsub/run
```

The bench sends `messagesPerPublisher` messages to each topic from each publisher, but always at least one message per topic (in case `messagesPerPublisher` < `numberOfTopics` - not sure about this) 

Timing is static, so each run will pretty much take the exact same time: ~10 seconds waiting for all subscriptions to disseminate, ~20 seconds to wait for all messages to reach the subscribers, and then a timeout of 30s for result collection. If the entire bench session takes more than 2 minutes it fails.

`numberOfTopics` determine the total number of topics published and subscribed to

The `numberOfPublishers` determine the total number of publishers across the cluster, the publishers will be divided evenly across the cluster nodes so it will need to be evenly divisible over the number of nodes. Each publisher will then publish a message to a random topic, every 10 ms when the bench has started.

The `numberOfSubscribers` determine how many subscribers each node starts, the topics are then evenly divided between the subscribers of a node. 


Note that if/when the benchmark breaks down because of too many topics or subscribers, it may not be able to clean up after itself and the cluster will have to be restarted for subsequent benchmarks.