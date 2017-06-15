#!/usr/bin/env bash

fix --verbose --concurrency 3 \
  nodes_with_role:benchmark-sharding \
  ssh:"cd /home/akka/apps; sudo -u akka /home/akka/sbt ';project sharding; run'"
