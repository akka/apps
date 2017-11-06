#!/usr/bin/env bash

# See https://issues.apache.org/jira/browse/CASSANDRA-4026 for this craziness
region=${REGION:=eu-central-1}
dc=${region::-2}
echo "Running in region: ${region} dc ${dc}"

aws_args="--region=$region"

cassandra_instances=$(aws ${aws_args} ec2 describe-instances --filters Name=tag:Purpose,Values=re --filter Name=tag:Role,Values=re-cassandra  --query 'Reservations[].Instances[].[PrivateIpAddress,PublicIpAddress,Tags[?Key==`Name`].Value[],Tags[?Key==`Role`].Value[]]' --output text   | xargs -n3 -d '\n')

akka_instances=$(aws ${aws_args} ec2 describe-instances --filters Name=tag:Purpose,Values=re --filter Name=tag:Role,Values=re-akka  --query 'Reservations[].Instances[].[PrivateIpAddress,PublicIpAddress,Tags[?Key==`Name`].Value[],Tags[?Key==`Role`].Value[]]' --output text   | xargs -n3 -d '\n' | grep -v None)


echo "Cassandra instances"
echo "$cassandra_instances"

echo "Akka instances"
echo "$akka_instances"

# Always get the seed from eu-west-1
cassandra_seeds=$(aws --region=eu-west-1 ec2 describe-instances --filters Name=tag:Purpose,Values=re --filter Name=tag:Role,Values=re-cassandra --query 'Reservations[].Instances[].[PublicIpAddress]' --output text)

read -r -a cassandra_seeds_arr <<< "$cassandra_seeds"
cassandra_seed="${cassandra_seeds_arr[0]}"
echo "Setting Cassandra seed to: $cassandra_seed"

akka_seeds=$(aws --region=eu-west-1 ec2 describe-instances --filters Name=tag:Purpose,Values=re --filter Name=tag:Role,Values=re-akka  --query 'Reservations[].Instances[].[PublicIpAddress]' --output text)

read -r -a akka_seeds_arr <<< "$akka_seeds"
akka_seed="${akka_seeds_arr[0]}"
echo "Setting Akka seed to: $akka_seed"

# Cassandra contact point should be local to the DC
# Always get the seed from eu-west-1
cassandra_contact_points=$(aws ${aws_args} ec2 describe-instances --filters Name=tag:Purpose,Values=re --filter Name=tag:Role,Values=re-cassandra --query 'Reservations[].Instances[].[PublicIpAddress]' --output text)

read -r -a cassandra_contact_arr <<< "$cassandra_contact_points"
cassandra_contact_point="${cassandra_contact_arr[0]}"
echo "Setting Cassandra contact_point to: $cassandra_contact_points"


echo "Generating files"
while read -r node; do
  echo "Processing node: $node"
  read -r -a array <<< "$node"
  internal_ip=${array[0]}
  external_ip=${array[1]}
  name=${array[2]}
  role=${array[3]}

  echo "Private IP: ${internal_ip}"
  echo "Public IP: ${external_ip}"
  echo "Name: ${name}"
  echo "Role: ${role}"

  echo "Generating file: ./node/$name.json"

  cat ./nodes/re-cassandra.json.template |
    sed "s/NAME/$name/g"               |
    sed "s/INTERNAL_IP/$internal_ip/g" |
    sed "s/EXTERNAL_IP/$external_ip/g" |
    sed "s/DC/$dc/g"                   |
    sed "s/AKKA_SEED_IP/$akka_seed/g"  |
    sed "s/CASSANDRA_CONTACT_IP/$cassandra_contact_point/g"  |
    sed "s/CASSANDRA_SEED_IP/$cassandra_seed/g" > ./nodes/${name}.json

  echo "Generated: ./nodes/$name.json"

done <<< "$cassandra_instances"

while read -r node; do
  echo "Processing node: $node"
  read -r -a array <<< "$node"
  internal_ip=${array[0]}
  external_ip=${array[1]}
  name=${array[2]}
  role=${array[3]}

  echo "Private IP: ${internal_ip}"
  echo "Public IP: ${external_ip}"
  echo "Name: ${name}"
  echo "Role: ${role}"

  echo "Generating file: ./nodes/$name.json"

  cat ./nodes/re-akka.json.template |
    sed "s/NAME/$name/g"               |
    sed "s/INTERNAL_IP/$internal_ip/g" |
    sed "s/EXTERNAL_IP/$external_ip/g" |
    sed "s/DC/$dc/g"                   |
    sed "s/AKKA_SEED_IP/$akka_seed/g"  |
    sed "s/CASSANDRA_CONTACT_IP/$cassandra_contact_point/g"  |
    sed "s/CASSANDRA_SEED_IP/$cassandra_seed/g" > ./nodes/${name}.json

  echo "Generated: ./nodes/$name.json"

done <<< "$akka_instances"


echo "Put the following in your /etc/hosts"

echo "#Cassandra nodes"
while read -r node; do
  read -r -a array <<< "$node"
  external_ip=${array[1]}
  name=${array[2]}
  echo "${external_ip} ${name}"

done <<< "$cassandra_instances"

echo "#Akka nodes"
while read -r node; do
  read -r -a array <<< "$node"
  external_ip=${array[1]}
  name=${array[2]}
  echo "${external_ip} ${name}"
done <<< "$akka_instances"





