# apps

## How to run locally

### Sharding

* Start Cassandra with default configuration
* Start 3 instances of the ShardingStartLatencyApp with different ports:
  * sbt -Dakka.remote.artery.canonical.hostname=127.0.0.1 -Dakka.remote.artery.canonical.port=2551 -Dshard-bench.minimum-nodes=3 -Dakka.cluster.seed-nodes.1=akka://ShardingStartLatencySystem@127.0.0.1:2551 "sharding/runMain com.lightbend.akka.bench.sharding.latency.ShardingStartLatencyApp"
  * sbt -Dakka.remote.artery.canonical.hostname=127.0.0.1 -Dakka.remote.artery.canonical.port=2552 -Dshard-bench.minimum-nodes=3 -Dakka.cluster.seed-nodes.1=akka://ShardingStartLatencySystem@127.0.0.1:2551 "sharding/runMain com.lightbend.akka.bench.sharding.latency.ShardingStartLatencyApp"
  * sbt -Dakka.remote.artery.canonical.hostname=127.0.0.1 -Dakka.remote.artery.canonical.port=2553 -Dshard-bench.minimum-nodes=3 -Dakka.cluster.seed-nodes.1=akka://ShardingStartLatencySystem@127.0.0.1:2551 "sharding/runMain com.lightbend.akka.bench.sharding.latency.ShardingStartLatencyApp"

The ShardingActorCountScalabilityApp is started in similar way:

* sbt -Dakka.remote.artery.canonical.hostname=127.0.0.1 -Dakka.remote.artery.canonical.port=2551 -Dshard-bench.minimum-nodes=3 -Dakka.cluster.seed-nodes.1=akka://ShardingActorCountScalabilitySystem@127.0.0.1:2551 "sharding/runMain com.lightbend.akka.bench.sharding.scalability.ShardingActorCountScalabilityApp"