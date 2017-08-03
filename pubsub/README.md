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
