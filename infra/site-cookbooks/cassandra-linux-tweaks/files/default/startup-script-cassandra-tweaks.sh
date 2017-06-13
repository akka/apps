#!/usr/bin/env bash

# This script will be executed as startup-script (see gcloud-new-akka-node.sh),
# by compute engine each time this node is started up. 

swapoff --all

sysctl net.core.rmem_max=16777216
sysctl net.core.wmem_max=16777216

sysctl net.core.rmem_default=16777216
sysctl net.core.wmem_default=16777216

sysctl net.core.optmem_max=40960    

sysctl net.ipv4.tcp_rmem="4096 87380 16777216"
sysctl net.ipv4.tcp_wmem="4096 65536 16777216"

echo 0 > /proc/sys/vm/zone_reclaim_mode
