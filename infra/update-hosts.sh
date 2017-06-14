#!/usr/bin/env bash

if [[ "$#" != "2" ]]; then
  echo "USAGE: ./update-hosts.sh node:akka-node-001 (which node to update) akka-node (which IPs to find and include)" 
  exit 1
fi

# selector should be `node:akka-node-001` or `nodes_with_role:benchmark`
declare -r SELECTOR=$1
# node-grep should be as simple as "akka-node" for example
declare -r NODE_GREP=$2

echo "Looking for akka-nodes..."

declare -r internal_ips=$(gcloud --project="akka-gcp" compute instances list | grep "$NODE_GREP" | awk '{ print $4 }')
declare -r external_ips=$(gcloud --project="akka-gcp" compute instances list | grep "$NODE_GREP" | awk '{ print $5 }')

rm ./internal-ips.tmp
for ip in $internal_ips; do
  echo $ip >> ./internal-ips.tmp
done

echo "# ----------------------------------------------"
echo "Found internal IPs, stored as ./internal-ips.tmp"
echo "# ----------------------------------------------"
cat ./internal-ips.tmp
echo "# ----------------------------------------------"
echo "IF YOU WANT, EDIT THAT FILE, WE WILL WRITE to all: $SELECTOR"
echo "# ----------------------------------------------"
read

cat ./internal-ips.tmp > site-cookbooks/update-hosts/templates/default/multi-node-test.hosts

declare -r PAR=5
echo "EXECUTING WITH PARALLELISM: $PAR"

# # usually not needed anymore:
#echo "Preparing node..."
#fix --concurrency $PAR $SELECTOR deploy_chef -y

echo "Updating node..."
fix --concurrency $PAR $SELECTOR recipe:update-hosts

#echo "Deploying Chef to [$node]..."
#knife solo prepare $NODE
echo "------------------- DONE -------------------"
