#!/usr/bin/env bash

# NOTE: watch out to keep \ the last character (no trailing space)

NAME="$1"

echo "Creating new node $NAME..."

gcloud compute --project "akka-cloud" \
  instances create "$NAME" \
  --zone "europe-west1-b" \
  --machine-type "n1-standard-1" --subnet "default" --maintenance-policy "MIGRATE" --service-account "47948532132-compute@developer.gserviceaccount.com" \
  --scopes "https://www.googleapis.com/auth/cloud-platform" \
  --tags "http-server","https-server" \
  --image "ubuntu-1704-zesty-v20170413" --image-project "ubuntu-os-cloud" \
  --boot-disk-size "10" --boot-disk-type "pd-standard" \
  --boot-disk-device-name "$NAME"


#echo "Deploying Chef to [$node]..."
#knife solo prepare $NODE
echo "------------------- DONE -------------------"
