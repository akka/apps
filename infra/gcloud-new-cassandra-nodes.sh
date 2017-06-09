#!/usr/bin/env bash

# easily spin up new nodes and configure them

# NOTE: watch out to keep \ the last character (no trailing space)

gcloud compute --project "akka-gcp" \
  instances create "akka-cassandra-001" \
  --zone "europe-west1-b" \
  --machine-type "n1-standard-4" \
  --subnet "default" --maintenance-policy "MIGRATE" --service-account "7250250762-compute@developer.gserviceaccount.com" \
  --scopes "https://www.googleapis.com/auth/cloud-platform" \
  --image "ubuntu-1704-zesty-v20170413" \
  --image-project "ubuntu-os-cloud" \
  --boot-disk-size "20" --boot-disk-type "pd-ssd" \
  --boot-disk-device-name "akka-cassandra-001"

gcloud compute --project "akka-gcp" \
  instances create "akka-cassandra-002" \
  --zone "europe-west1-b" \
  --machine-type "n1-standard-4" \
  --subnet "default" --maintenance-policy "MIGRATE" --service-account "7250250762-compute@developer.gserviceaccount.com" \
  --scopes "https://www.googleapis.com/auth/cloud-platform" \
  --image "ubuntu-1704-zesty-v20170413" \
  --image-project "ubuntu-os-cloud" \
  --boot-disk-size "20" --boot-disk-type "pd-ssd" \
  --boot-disk-device-name "akka-cassandra-002"
  
gcloud compute --project "akka-gcp" \
  instances create "akka-cassandra-003" \
  --zone "europe-west1-b" \
  --machine-type "n1-standard-4" \
  --subnet "default" --maintenance-policy "MIGRATE" --service-account "7250250762-compute@developer.gserviceaccount.com" \
  --scopes "https://www.googleapis.com/auth/cloud-platform" \
  --image "ubuntu-1704-zesty-v20170413" \
  --image-project "ubuntu-os-cloud" \
  --boot-disk-size "20" --boot-disk-type "pd-ssd" \
  --boot-disk-device-name "akka-cassandra-003"

# get all IPs

declare -r cassandra_001_internal_ip=$(gcloud --project="akka-gcp" compute instances list | grep "cassandra-001" | head -n1 | awk '{ print $4 }')
declare -r cassandra_001_external_ip=$(gcloud --project="akka-gcp" compute instances list | grep "cassandra-001" | head -n1 | awk '{ print $5 }')

declare -r cassandra_002_internal_ip=$(gcloud --project="akka-gcp" compute instances list | grep "cassandra-002" | head -n1 | awk '{ print $4 }')
declare -r cassandra_002_external_ip=$(gcloud --project="akka-gcp" compute instances list | grep "cassandra-002" | head -n1 | awk '{ print $5 }')

declare -r cassandra_003_internal_ip=$(gcloud --project="akka-gcp" compute instances list | grep "cassandra-003" | head -n1 | awk '{ print $4 }')
declare -r cassandra_003_external_ip=$(gcloud --project="akka-gcp" compute instances list | grep "cassandra-003" | head -n1 | awk '{ print $5 }')

# prepare node descriptions
cat nodes/cassandra.json.template |
  sed "s/NAME/$cassandra_001_external_ip/g" |   
  sed "s/CASSANDRA_SEED_IP/$cassandra_001_internal_ip/g" |  
  sed "s/EXTERNAL_IP/$cassandra_001_external_ip/g" | 
  sed "s/INTERNAL_IP/$cassandra_001_internal_ip/g" > nodes/cassandra-001.json
cat nodes/cassandra.template | 
  sed "s/NAME/$cassandra_002_external_ip/g" | 
  sed "s/CASSANDRA_SEED_IP/$cassandra_001_internal_ip/g" |  
  sed "s/EXTERNAL_IP/$cassandra_002_external_ip/g" | 
  sed "s/INTERNAL_IP/$cassandra_002_internal_ip/g" > nodes/cassandra-002.json
cat nodes/cassandra.template | 
  sed "s/NAME/$cassandra_003_external_ip/g" |    
  sed "s/CASSANDRA_SEED_IP/$cassandra_001_internal_ip/g" |  
  sed "s/EXTERNAL_IP/$cassandra_003_external_ip/g" | 
  sed "s/INTERNAL_IP/$cassandra_003_internal_ip/g" > nodes/cassandra-003.json

# allow CQL access to nodes
gcloud beta compute --project "akka-gcp" firewall-rules create "allow-cassandra" --allow tcp:9042 --direction "INGRESS" --priority "1000" --network "default" --source-ranges "0.0.0.0" --target-tags "cassandra"


echo "------ UPDATE YOUR /etc/hosts -------"
echo "# -----------------------------------"
echo "# akka cluster on GCE"
echo "$cassandra_001_external_ip cassandra-001"
echo "$cassandra_002_external_ip cassandra-002"
echo "$cassandra_003_external_ip cassandra-003"
echo "# -----------------------------------"
echo "      Press ENTER when ready...      "
read
echo "# -----------------------------------"

# chef all cassandra nodes
echo "Deploying chef to all nodes"
fix node:cassandra-001 deploy_chef -y
fix node:cassandra-002 deploy_chef -y
fix node:cassandra-003 deploy_chef -y

# cook all nodes!

fix nodes_with_role:cassandra

echo "------------------- DONE -------------------"
