#!/usr/bin/env bash

# easily spin up new nodes and configure them

# NOTE: watch out to keep \ the last character (no trailing space)


# fix config on given node:
#
for node_number in 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20
do
#  ssh 10.128.0.${node_number}
#  
#     sudo service cassandra stop
#     sudo rm -rf /var/lib/cassandra/data/
#     sudo mkdir /var/lib/cassandra/data
#     sudo chown -R cassandra.cassandra /var/lib/cassandra
#     sudo sed -i  "s/10.128.0.5/$(ifconfig  | grep 10.128 | awk '{ print $2 }')/g" /etc/cassandra/cassandra.yaml
#     sudo update-rc.d cassandra enable
#     sudo service cassandra start
#done

#  #!/bin/bash
#  cd /home/akka
#  echo \"Running all startup-scripts in $(pwd)...\" 
#  for script in $(ls *startup-script*); do
#    echo \"Running $script\"
#    ./$script
#  done

  gcloud compute --project "akka-gcp" disks create "akka-cassandra-${node_number}" --size "20" \
  --zone "us-central1-a" --source-snapshot "akka-cassandra-node-snapshot" --type "pd-ssd"

  gcloud compute --project "akka-gcp" instances create "akka-cassandra-${node_number}" \
  --zone "us-central1-a" --machine-type "n1-standard-16" \
  --subnet "default" --metadata 'startup-script=#!/bin/bash\u000a  cd /home/akka\u000a  echo \\\"Running all startup-scripts in $(pwd)...\\\" \u000a  for script in $(ls *startup-script*); do\u000a    echo \\\"Running $script\\\"\u000a    ./$script\u000a  done' \
  --maintenance-policy "MIGRATE" --service-account "7250250762-compute@developer.gserviceaccount.com" \
  --scopes "https://www.googleapis.com/auth/cloud-platform" \
  --disk "name=akka-cassandra-${node_number},device-name=akka-cassandra-${node_number},mode=rw,boot=yes,auto-delete=yes" \
  --no-address

  # get all IP
  
  cassandra_internal_ip=$(gcloud --project="akka-gcp" compute instances list | grep "cassandra-$node_number" | head -n1 | awk '{ print $4 }')
  cassandra_external_ip=$(gcloud --project="akka-gcp" compute instances list | grep "cassandra-$node_number" | head -n1 | awk '{ print $5 }')
  
  # prepare node descriptions
  cat nodes/akka-cassandra.json.template |
    sed "s/NAME/$cassandra_internal_ip/g" |   
    sed "s/CASSANDRA_SEED_IP/10.128.0.4/g" |  
    sed "s/EXTERNAL_IP/$cassandra_external_ip/g" | 
    sed "s/INTERNAL_IP/$cassandra_internal_ip/g" > nodes/akka-cassandra-${node_number}.json
    
  echo "Created nodes/cassandra-${node_number}.json..."
  
done

## allow CQL access to nodes
#echo "Allowing network connections..."
#gcloud beta compute --project "akka-gcp" firewall-rules create "allow-cassandra" --allow tcp:9042 --direction "INGRESS" --priority "1000" --network "default" --source-ranges "0.0.0.0" --target-tags "cassandra"

#
#echo "# -----------------------------------"
#echo "# akka cluster on GCE"
#echo ""
#cat ./cassandra-hosts
#echo ""
#echo "------ UPDATE YOUR /etc/hosts -------"
#echo "# -----------------------------------"
#echo "      Press ENTER when ready...      "
#read
#echo "# -----------------------------------"
#
## chef all cassandra nodes
#echo "Deploying chef to all nodes"
#for node_number in 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20
#do 
#  fix node:cassandra-${node_number} deploy_chef -y
#done
#
## cook all nodes!
#
#fix nodes_with_role:cassandra
#
#echo "------------------- DONE -------------------"



for n in 09 10 11 12 13 14 15 16 17 18 19 20; do
gcloud compute --project "akka-gcp" disks create "akka-cassandra-$n" --size "20" --zone "us-central1-a" --source-snapshot "cassandra-base" --type "pd-ssd"

gcloud compute --project "akka-gcp" instances create "akka-cassandra-$n" --zone "us-central1-a" --machine-type "n1-standard-16" --subnet "default" --no-address --metadata "startup-script=ip a | grep 10.128 | awk \"{print $4}\" > /home/ktoso/ip\u000asudo sed -i \"s/rpc_address: 10.128.0.4/rpc_address: $(cat /home/ktoso/ip)/g\" /etc/cassandra/cassandra.yaml\u000asudo sed -i \"s/listen_address: 10.128.0.4/listen_address: $(cat /homne/ktoso/ip)/g\" /etc/cassandra/cassandra.yaml" --maintenance-policy "MIGRATE" --service-account "7250250762-compute@developer.gserviceaccount.com" --scopes "https://www.googleapis.com/auth/cloud-platform" --tags "cassandra","akka" --disk "name=akka-cassandra-$n,device-name=akka-cassandra-$n,mode=rw,boot=yes,auto-delete=yes"

done


for IP2 in 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20; do
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no akka-cassandra-$IP2 "
  # sudo rm -rf /var/lib/cassandra/*
  # ip a | grep 10.128 | awk '{print \$4}' > ip
  # sudo sed -i \"s/rpc_address: 10.128.0.4/rpc_address: "$(cat /home/ktoso/ip)"/g\" /etc/cassandra/cassandra.yaml
  # sudo sed -i \"s/listen_address: 10.128.0.4/listen_address: "$(cat /home/ktoso/ip)"/g\" /etc/cassandra/cassandra.yaml
  sudo service cassandra start
"
done


sudo service cassandra stop
sudo rm -rf /var/lib/cassandra/*
ip a | grep 10.128 | awk '{print $4}' > ip
sudo sed -i "s/rpc_address: 10.128.0.4/rpc_address: "$(cat /home/ktoso/ip)"/g" /etc/cassandra/cassandra.yaml
sudo sed -i "s/listen_address: 10.128.0.4/listen_address: "$(cat /home/ktoso/ip)"/g" /etc/cassandra/cassandra.yaml
sudo service cassandra start
