#!/bin/sh

# examples:
#
# ./make-node-files-from-gcloud.sh akka-node-01 '["10.154.0.2"]'
#
# EXTRA_RUNLIST='"role[benchmark-ddata]",' ./make-node-files-from-gcloud.sh akka-node-01 '["10.154.0.2"]'

if [[ "$#" != "2" ]]; then
  echo "USAGE: ./make-node-files-from-gcloud.sh GREP SEED_NODES"
fi

declare -r GREP=$1
declare -r SEED_NODES=$2

declare -r TOTAL_NODES=$(gcloud compute instances list | grep $GREP | wc -l | awk '{print $1}')


IFS=$'\n'       # make newlines the only separator
for node in $(gcloud compute instances list | grep $GREP | awk ' { print $1,$4,$5 } ')
do
  name=$(echo $node | awk ' { print $1} ')
  internal_ip=$(echo $node | awk ' { print $2} ')
  external_ip=$(echo $node | awk ' { print $3} ')
  
  cat ./nodes/akka-node.json.template |
    sed "s/NAME/$name/g" |
    sed "s/INTERNAL_IP/$internal_ip/g" |
    sed "s/AKKA_SEED_NODES/$SEED_NODES/g" |
    sed "s/EXTRA_RUNLIST/$EXTRA_RUNLIST/g" |
    sed "s/TOTAL_NODES/$TOTAL_NODES/g" |
    sed "s/EXTERNAL_IP/$external_ip/g" > ./nodes/$name.json 
    
    echo "Generated: ./nodes/$name.json" 
done
