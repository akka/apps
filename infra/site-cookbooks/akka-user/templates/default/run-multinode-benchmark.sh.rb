#!/usr/bin/env bash

declare -r LOGS_BASE="/home/akka/log/$(date '+20%y-%m-%d--%H%M')"

for ip in $(cat multi-node-test.hosts); do
  mkdir -p $LOGS_BASE/$ip
done

declare -r AERON_DIR=/dev/shm/aeron-akka

declare -r MEDIA_DRIVER_ARGS="-Xms1g -Xmx1g -XX:+UseCompressedOops -XX:MaxDirectMemorySize=256m -XX:ReservedCodeCacheSize=256m -XX:BiasedLockingStartupDelay=0 -Daeron.mtu.length=16384 -Daeron.socket.so_sndbuf=2097152 -Daeron.socket.so_rcvbuf=2097152 -Daeron.rcv.initial.window.length=2097152 -Dagrona.disable.bounds.checks=true"

declare -r MULTI_NODE_DIR="/home/akka/tmp-akka-multi-node"
declare -r MULTI_NODE_ARGS="-Dakka.test.multi-node=true -Dakka.test.multi-node.targetDirName=$MULTI_NODE_DIR -Dmultinode.Xms1024M -Dmultinode.Xmx1024M -Dmultinode.XX:+PrintGCDetails -Dmultinode.XX:+PrintGCTimeStamps -Dmultinode.XX:BiasedLockingStartupDelay=0 -Dmultinode.Daeron.mtu.length=16384 -Dmultinode.Daeron.rcv.buffer.length=16384 -Dmultinode.Daeron.socket.so_sndbuf=2097152 -Dmultinode.Daeron.socket.so_rcvbuf=2097152 -Dmultinode.Daeron.rcv.initial.window.length=2097152 -Dmultinode.Dagrona.disable.bounds.checks=true -Dmultinode.XX:+UseCompressedOops -Dmultinode.XX:MaxDirectMemorySize=256m -Dmultinode.XX:+UnlockDiagnosticVMOptions -Dmultinode.XX:GuaranteedSafepointInterval=300000"
declare -r COMMON_ARGS="-Dakka.test.LatencySpec.totalMessagesFactor=30 -Dakka.test.MaxThroughputSpec.totalMessagesFactor=200 -Dakka.remote.artery.advanced.embedded-media-driver=off -Dakka.remote.artery.advanced.aeron-dir=$AERON_DIR"
#declare -r COMMON_ARGS="-Dakka.remote.artery.advanced.embedded-media-driver=off -Dakka.remote.artery.advanced.aeron-dir=$AERON_DIR"

#declare -r ARGS="-Dakka.remote.artery.advanced.inbound-lanes=1"
declare -r ARGS="$1"

mkdir -p $LOGS1
ssh $IP2 "cd $PWD; mkdir -p $LOGS2"

# description of test
echo "$1" > $LOGS_BASE/readme.txt
git log -n 1 >> $LOGS_BASE/readme.txt



for ip in $(cat multi-node-test.hosts); do
  prepare $ip
done

# run the tests
cd /home/akka/akka
/home/akka/sbt $ARGS $COMMON_ARGS $MULTI_NODE_ARGS 'akka-remote-tests/test' > $LOGS1/log.txt
cd /home/akka/


# -------------- functions --------------

# $1 - IP to execute scripts on
prepare() {
  declare -r IP2="$1"
  
  rm -rf $AERON_DIR
  ssh $IP2 "cd $PWD; rm -rf $AERON_DIR"
  
  mkdir -p /home/akka/aeron; cd /home/akka/aeron; wget -nc http://repo1.maven.org/maven2/io/aeron/aeron-all/1.2.5/aeron-all-1.2.5.jar
  ssh $IP2 "mkdir -p /home/akka/aeron; cd /home/akka/aeron; wget -nc http://repo1.maven.org/maven2/io/aeron/aeron-all/1.2.5/aeron-all-1.2.5.jar"
  
  java -cp /home/akka/aeron/aeron-all-1.2.5.jar $MEDIA_DRIVER_ARGS io.aeron.driver.MediaDriver &
  ssh $IP2 "java -cp /home/akka/aeron/aeron-all-1.2.5.jar $MEDIA_DRIVER_ARGS io.aeron.driver.MediaDriver" &
  sleep 10
  
  java -cp /home/akka/aeron/aeron-all-1.2.5.jar -Daeron.dir=$AERON_DIR io.aeron.samples.AeronStat type=[1-9] > $LOGS1/aeron-stat.txt &
  ssh $IP2 "java -cp /home/akka/aeron/aeron-all-1.2.5.jar -Daeron.dir=$AERON_DIR io.aeron.samples.AeronStat type=[1-9] > $LOGS2/aeron-stat.txt" &
  
  vmstat 1 > $LOGS1/vmstat.txt &
  ssh $IP2 "cd $PWD; vmstat 1 > $LOGS2/vmstat.txt" &
}

# $1 - IP to execute scripts on
finish() {
  declare -r IP2="$1"
  
  java -cp /home/akka/aeron/aeron-all-1.2.5.jar -Daeron.dir=$AERON_DIR io.aeron.samples.ErrorStat > $LOGS1/aeron-err.txt
  ssh $IP2 "java -cp /home/akka/aeron/aeron-all-1.2.5.jar -Daeron.dir=$AERON_DIR io.aeron.samples.ErrorStat > $LOGS2/aeron-err.txt"
  
  cp -R $AERON_DIR $LOGS1
  ssh $IP2 "cd $PWD; cp -R $AERON_DIR $LOGS2"
  
  pkill -u akka vmstat
  ssh $IP2 "cd $PWD; pkill -u akka vmstat"
  
  pkill -u akka -f aeron
  ssh $IP2 "cd $PWD; pkill -u akka -f aeron"
  
  scp -r $IP2:$LOGS2 $LOGS_BASE/
}

for ip in $(cat multi-node-test.hosts); do
  finish $ip
done
