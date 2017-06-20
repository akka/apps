#!/usr/bin/env bash

declare -r CLAZZ=com.lightbend.akka.bench.ddata.DistributedDataBenchmark

fix --verbose --concurrency 2 \
  nodes_with_role:benchmark-ddata \
  ssh:"cd /home/akka/apps; sudo -u akka /home/akka/sbt ';project apps; runMain $CLAZZ'"
