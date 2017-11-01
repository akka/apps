#!/bin/bash

# pull in the node ip addresses
. nodes-bash-exports.sh

for node in "${nodes_all[@]}"; do
  echo "============ Killing MultiDC App on:       $node     ... ============"
  ssh -t -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i $HOME/.ssh/replicated-entity.pem akka@${node} "
    killall -9 java
    rm -rf /dev/shm/*
  "


done

ps aux  | grep ssh | grep trictHostKeyChecking=no | awk '{ print $2 }' | xargs kill
