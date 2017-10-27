#!/bin/bash -x

# pull in the node ip addresses
. nodes-bash-exports.sh

if [[ "$2" -eq "split" ]]; then
  echo "========= PARTITION FULL SPLIT... ========="

  for node in "${nodes_central[@]}"; do
    echo "============ Separating Central node:       $node     ... ============"

    for separate_from_node in "${nodes_west_ip}"; do
      ssh -i $HOME/.ssh/replicated-entity.pem ubuntu@${node} "sudo iptables -A INPUT -p udp -s $separate_from_node -j DROP" &
    done

  done

#  # uncomment if you want a "full split"
#  for node in "${nodes_west[@]}"; do
#    echo "============ Separating West node:       $node     ... ============"
#    for separate_from_node in "${nodes_central_ip}"; do
#      ssh -i $HOME/.ssh/replicated-entity.pem ubuntu@${node} "sudo iptables -A INPUT -p udp -s $separate_from_node -j DROP" &
#    done
#  done

elif [[ "$2" -eq "heal" ]]; then
  echo "========= HEAL $2 to rejoin other nodes... ========="

  for node in "${nodes_all[@]}"; do
    echo "============ Healing node:       $node     ... ============"

    for separate_from_node in "${nodes_all}"; do
      ssh -i $HOME/.ssh/replicated-entity.pem ubuntu@${node} "sudo iptables -A INPUT -p udp -s $separate_from_node -j ACCEPT" &
    done

  done

else
  echo "Usage: network-split.sh [split | heal]"
fi
