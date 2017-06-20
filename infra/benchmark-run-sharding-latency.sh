#!/usr/bin/env bash

declare -r CLAZZ="com.lightbend.akka.bench.sharding.ShardingLatencyApp"

fix --verbose --concurrency 1000 \
  nodes_with_role:benchmark-sharding \
  ssh:"cd /home/akka/apps; sudo -u akka /home/akka/sbt ';project sharding; runMain $CLAZZ'"
