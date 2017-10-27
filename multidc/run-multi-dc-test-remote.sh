#!/bin/bash

# pull in the node ip addresses
. nodes-bash-exports.sh

for node in "${nodes_all[@]}"; do
  echo "============ Staging MultiDC App on:       $node     ... ============"
  ssh -t -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i $HOME/.ssh/replicated-entity.pem akka@${node} "
    cd multidc/apps
    pwd

    killall -9 java
    sleep 1
    /home/akka/sbt '; project multidc; runMain com.lightbend.multidc.ReplicatedEntityApp '
  " &

done

##curl -v "$DC1/members" | jq
##
##curl -v "$DC1/single-counter-test?counter=first&updates=100"
##
###curl -v "$DC1/single-counter-test?counter=first&updates=100"
##
##curl -v "$DC1/counter?id=first"
##
##curl -v "$DC1/test?counters=100&updates=100"
##curl -v "$DC1/counter?id=99"
#
#
