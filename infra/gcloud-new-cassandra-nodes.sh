#!/usr/bin/env bash

# easily spin up new nodes and configure them

# NOTE: watch out to keep \ the last character (no trailing space)

rm ./cassandra-hosts

for node_number in 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20
do

  gcloud compute --project "akka-gcp" instances create "akka-cassandra-$node_number" \
  --zone "us-central1-a" \
  --machine-type "n1-standard-16" --subnet "default" \
  --metadata 'startup-script=#! /bin/bash\u000a    cd /home/akka\u000a    echo \"Running all startup-scripts in $(pwd)...\" \u000a    for script in $(ls *startup-script*); do\u000a      echo \"Running $script\"\u000a      ./$script\u000a    done'\
   --maintenance-policy "MIGRATE" \
   --service-account "7250250762-compute@developer.gserviceaccount.com" \
   --scopes "https://www.googleapis.com/auth/cloud-platform" \
   --tags "cassandra" --image "ubuntu-1704-zesty-v20170413" \
   --image-project "ubuntu-os-cloud" \
   --boot-disk-size "20" --boot-disk-type "pd-ssd" \
   --boot-disk-device-name "akka-cassandra-$node_number" \
   --no-address
  
  
  # get all IP
  
  cassandra_internal_ip=$(gcloud --project="akka-gcp" compute instances list | grep "cassandra-$node_number" | head -n1 | awk '{ print $4 }')
  cassandra_external_ip=$(gcloud --project="akka-gcp" compute instances list | grep "cassandra-$node_number" | head -n1 | awk '{ print $5 }')
  
  echo ${cassandra_external_ip} >> ./cassandra-hosts
  
  # prepare node descriptions
  cat nodes/cassandra.json.template |
    sed "s/NAME/$cassandra_external_ip/g" |   
    sed "s/CASSANDRA_SEED_IP/10.128.0.3/g" |  
    sed "s/EXTERNAL_IP/$cassandra_external_ip/g" | 
    sed "s/INTERNAL_IP/$cassandra_internal_ip/g" > nodes/cassandra-${node_number}.json
    
  echo "Created nodes/cassandra-${node_number}.json..."
  
done

## allow CQL access to nodes
#echo "Allowing network connections..."
#gcloud beta compute --project "akka-gcp" firewall-rules create "allow-cassandra" --allow tcp:9042 --direction "INGRESS" --priority "1000" --network "default" --source-ranges "0.0.0.0" --target-tags "cassandra"


echo "# -----------------------------------"
echo "# akka cluster on GCE"
echo ""
cat ./cassandra-hosts
echo ""
echo "------ UPDATE YOUR /etc/hosts -------"
echo "# -----------------------------------"
echo "      Press ENTER when ready...      "
read
echo "# -----------------------------------"

# chef all cassandra nodes
echo "Deploying chef to all nodes"
for node_number in 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20
do 
  fix node:cassandra-${node_number} deploy_chef -y
done

# cook all nodes!

fix nodes_with_role:cassandra

echo "------------------- DONE -------------------"
