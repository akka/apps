#!/bin/bash

# pull in the node ip addresses
. nodes-cassandra-bash-exports.sh

echo "======= $1 ======="

if [[ "$1" = "split" ]];
then
  echo "========= PARTITION FULL SPLIT... ========="

  for node in "${nodes_cassandra_central_ip[@]}"; do
    echo "============ Separating Central node:       $node     ... ============"
    for separate_from_node in "${nodes_cassandra_west_ip[@]}"; do
      echo "Splitting $node from $separate_from_node ... "
      ssh -i $HOME/.ssh/re-central.pem ubuntu@${node} "sudo iptables -A INPUT -p tcp -s $separate_from_node -j DROP"
    done
  done

  # uncomment if you want a "full split"
  for node in "${nodes_cassandra_west_ip[@]}"; do
    echo "============ Separating West node:       $node     ... ============"
    for separate_from_node in "${nodes_cassandra_central_ip[@]}"; do
      echo "Splitting $node from $separate_from_node ... "
      ssh -i $HOME/.ssh/replicated-entity.pem ubuntu@${node} "sudo iptables -A INPUT -p tcp -s $separate_from_node -j DROP"
    done
  done

elif [[ "$1" = "heal" ]];
then
  echo "========= HEAL $2 to rejoin other nodes... ========="

  for node in "${nodes_cassandra_all_ip[@]}"; do
    echo "============ Healing node:       $node     ... ============"
    ssh -i $HOME/.ssh/re-central.pem ubuntu@${node} "sudo iptables -F"
    ssh -i $HOME/.ssh/replicated-entity.pem ubuntu@${node} "sudo iptables -F"
  done

else
  echo "Usage: network-split-cassandra.sh [split | heal]"
fi
