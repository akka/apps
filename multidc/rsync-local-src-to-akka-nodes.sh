#!/bin/bash -x

# pull in the node ip addresses
. nodes-bash-exports.sh

for node in "${nodes_all[@]}"; do
  echo "============ syncing with:       $node     ... ============"

  ssh -i $HOME/.ssh/replicated-entity.pem akka@${node} "rm -rf multidc/apps/build.sbt"
  rsync -Pavuze "ssh -i $HOME/.ssh/replicated-entity.pem" ../build.sbt akka@${node}:multidc/apps/build.sbt &

  ssh -i $HOME/.ssh/replicated-entity.pem akka@${node} "rm -rf multidc/apps/multidc/build.sbt"
  rsync -Pavuze "ssh -i $HOME/.ssh/replicated-entity.pem" build.sbt akka@${node}:multidc/apps/multidc/build.sbt &

  ssh -i $HOME/.ssh/replicated-entity.pem akka@${node} "rm -rf multidc/apps/multidc/src"
  rsync -Pavuze "ssh -i $HOME/.ssh/replicated-entity.pem" src akka@${node}:multidc/apps/multidc/ &

done
