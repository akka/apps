#!/bin/bash

# pull in the node ip addresses
. nodes-bash-exports.sh

for node in "${nodes_all[@]}"; do
  echo "============ Staging MultiDC App on:       $node     ... ============"
  scp -t -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i $HOME/.ssh/replicated-entity.pem \
    akka@${node}:multidc/apps/replicated-entity.log ${node}.log

done
