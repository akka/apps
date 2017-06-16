#!/usr/bin/env bash

declare -r CLAZZ="com.lightbend.akka.bench.sharding.InitCassandraApp"

fix --verbose node:akka-cassandra-001 ssh:"cd /home/akka/apps; sudo -u akka /home/akka/sbt ';project sharding; runMain $CLAZZ'"
