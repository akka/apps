#!/usr/bin/env bash

# NOTE: watch out to keep \ the last character (no trailing space)

for node_number in 002 003 004 005 006 007 008 009 010
do
  echo "Creating new node akka-sharding-${node_number}..."
  gcloud compute --project "akka-gcp" disks create "akka-sharding-${node_number}" --size "10" --zone "us-central1-a" \
  --source-snapshot "akka-sharding-snapshot" --type "pd-standard"
  
  gcloud compute --project "akka-gcp" instances create "akka-sharding-${node_number}" --zone "us-central1-a" \
  --machine-type "n1-standard-4" --subnet "default" \
  --no-address\
  --maintenance-policy "MIGRATE" --service-account "7250250762-compute@developer.gserviceaccount.com" \
  --scopes "https://www.googleapis.com/auth/devstorage.read_only","https://www.googleapis.com/auth/logging.write","https://www.googleapis.com/auth/monitoring.write","https://www.googleapis.com/auth/servicecontrol","https://www.googleapis.com/auth/service.management.readonly","https://www.googleapis.com/auth/trace.append" \
  --tags "sharding","akka" --disk "name=akka-sharding-${node_number},device-name=akka-sharding-${node_number},mode=rw,boot=yes,auto-delete=yes"

done
