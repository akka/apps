#!/usr/bin/env bash

# This script will be executed as startup-script (see gcloud-new-akka-node.sh),
# by compute engine each time this node is started up. 

swapoff --all

sysctl net.core.rmem_max=2097152
