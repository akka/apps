#!/usr/bin/env bash

# NOTE: watch out to keep \ the last character (no trailing space)

NAME="$1"
SEED="$2"

echo "Creating new node $NAME..."

# we request the min cpu to be Skylake:
# https://cloud.google.com/compute/docs/instances/specify-min-cpu-platform

gcloud compute --project "akka-gcp" \
  instances create "$NAME" \
  --zone "europe-west1-b" \
  --machine-type "n1-standard-4" \
  --min-cpu-platform "Intel Skylake" \
  --subnet "default" --maintenance-policy "MIGRATE" --service-account "7250250762-compute@developer.gserviceaccount.com" \
  --scopes "https://www.googleapis.com/auth/cloud-platform" \
  --image "ubuntu-1704-zesty-v20170413" \
  --image-project "ubuntu-os-cloud" \
  --tags "akka","http-server","https-server" \
  --boot-disk-size "20" --boot-disk-type "pd-ssd" \
  --boot-disk-device-name "$NAME" \
  --metadata startup-script='#!/bin/bash
    # Each time this node starts, it should attempt running the startup-script provided
    # Chef scripts prepare the individual scripts, we just make sure we run them on boot
    
    cd /home/akka
    echo "Running all startup-scripts in $(pwd)..." 
    for script in $(ls *startup-script*); do
      echo "Running $script"
      #./$script
    done
  '

declare -r internal_ip=$(gcloud --project="akka-gcp" compute instances list | grep "$NAME" | head -n1 | awk '{ print $4 }')
declare -r external_ip=$(gcloud --project="akka-gcp" compute instances list | grep "$NAME" | head -n1 | awk '{ print $5 }')

cat nodes/akka-node.json.template |
  sed "s/NAME/$NAME/g" |   
  sed "s/AKKA_SEED_NODES/$SEED/g" |  
  sed "s/EXTERNAL_IP/$external_ip/g" | 
  sed "s/INTERNAL_IP/$internal_ip/g" > nodes/$NAME.json
  
  
echo "# -----------------------------------"
echo "Check the file: ./nodes/$NAME.json"
echo "# -----------------------------------"
read
  

echo "# -----------------------------------"
echo "Seed nodes set to: $SEED"
echo "# -----------------------------------"

echo "------ UPDATE YOUR /etc/hosts -------"
echo "# -----------------------------------"
echo "$external_ip $NAME"
echo "# -----------------------------------"
echo "      Press ENTER when ready...      "
read
echo "# -----------------------------------"

echo "Deploying chef..."
fix node:$NAME deploy_chef -y

echo "Preparing node..."
fix node:$NAME

#echo "Deploying Chef to [$node]..."
#knife solo prepare $NODE
echo "------------------- DONE -------------------"
