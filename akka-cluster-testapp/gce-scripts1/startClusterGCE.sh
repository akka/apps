#!/bin/sh

MAX=1500
BATCH=20

startSeed1() {
  # first time: use --persistent_boot_disk --image=projects/debian-cloud/global/images/debian-7-wheezy-v20130816 instead of --disk=n0001,boot
  gcutil addinstance n0001 --disk=n0001,boot --nosynchronous_mode --zone=europe-west1-a --machine_type=n1-standard-2 --kernel=projects/google/global/kernels/gce-v20130813 --disk=akka-testapp,mode=read_only --disk=akka-logs,mode=read_write --metadata_from_file=startup-script:gce-scripts/startupSeed1.sh
}

startSeed2() {
  gcutil addinstance n0002 --nopersistent_boot_disk --nosynchronous_mode --zone=europe-west1-a --machine_type=n1-standard-2 --image=projects/debian-cloud/global/images/debian-7-wheezy-v20130816 --kernel=projects/google/global/kernels/gce-v20130813 --disk=akka-testapp,mode=read_only --metadata_from_file=startup-script:gce-scripts/startupSeed2.sh
}

startSeed3() {
  gcutil addinstance n0003 --nopersistent_boot_disk --nosynchronous_mode --zone=europe-west1-a --machine_type=n1-standard-2 --image=projects/debian-cloud/global/images/debian-7-wheezy-v20130816 --kernel=projects/google/global/kernels/gce-v20130813 --disk=akka-testapp,mode=read_only --metadata_from_file=startup-script:gce-scripts/startupSeed3.sh
}

startOneInstance() {
  gcutil addinstance n$N --nopersistent_boot_disk --nosynchronous_mode --zone=europe-west1-a --machine_type=n1-standard-2 --image=projects/debian-cloud/global/images/debian-7-wheezy-v20130816 --kernel=projects/google/global/kernels/gce-v20130813 --disk=akka-testapp,mode=read_only --external_ip_address=none --metadata_from_file=startup-script:gce-scripts/startup.sh &
}

startInstances() {
  local from=$1
  local to=$1+$2
  for ((j=$from; j<$to; j++)); do
  	local N=`printf "%04d\n" "$j"`
  	echo "Starting instance n$N"
    startOneInstance
  done
}

loop() {
  for ((i=$1; i<MAX; i+=BATCH)); do
    wait
  	startInstances $i $BATCH    
  done
}

wait() {
  #echo "Starting more instances in 3 minutes"
  sleep 120
  #echo "Starting more instances in 1 minute"
  sleep 60
}




startSeed1
startSeed2
startSeed3
firstBatch=$BATCH-3
startInstances 4 $firstBatch
loopStart=$BATCH+1
loop $loopStart

gcutil getproject --project=typesafe-akka --cache_flag_values

