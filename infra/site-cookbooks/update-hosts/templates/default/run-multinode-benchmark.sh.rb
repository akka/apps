#!/usr/bin/env bash

# -------------- functions -------------------------------------------------------------------------------------------------------

# $1 - IP to execute scripts on
prepare() {
  declare -r IP2="$1"
  declare -r LOGS2="$LOGS_BASE/$IP2"

  ssh $IP2 "
    cd $PWD

    mkdir -p $LOGS2

    rm -rf $AERON_DIR

    rm -f $MULTI_NODE_DIR/target/*.afr

    cat /proc/cpuinfo > $LOGS2/cpuInfo.txt

    mkdir -p /home/akka/aeron
    cd /home/akka/aeron
    wget -nc http://repo1.maven.org/maven2/io/aeron/aeron-all/1.2.5/aeron-all-1.2.5.jar

    java -cp /home/akka/aeron/aeron-all-1.2.5.jar $MEDIA_DRIVER_ARGS $ARGS io.aeron.driver.MediaDriver &
    sleep 10
    
    java -cp /home/akka/aeron/aeron-all-1.2.5.jar -Daeron.dir=$AERON_DIR io.aeron.samples.AeronStat type=[1-9] > $LOGS2/aeron-stat.txt &

    vmstat 1 > $LOGS2/vmstat.txt &
  " &

}

# $1 - IP to execute scripts on
finish() {
  declare -r IP2="$1"
  declare -r LOGS2="$LOGS_BASE/$IP2"

  ssh $IP2 "
    cd $PWD
  
    java -cp /home/akka/aeron/aeron-all-1.2.5.jar -Daeron.dir=$AERON_DIR io.aeron.samples.ErrorStat > $LOGS2/aeron-err.txt

    du -h /dev/shm/aeron-akka > $LOGS2/aeron-disk-usage.txt
  
    cp -R $AERON_DIR $LOGS2

    cp $MULTI_NODE_DIR/target/*.afr $LOGS2/
  
    pkill -u akka vmstat
  
    pkill -u akka java
    sleep 5
    pkill -9 -u akka java
  "
  
  # avoid copy own files
  if [ ! -d "$LOGS_BASE/$IP2" ]; then
    scp -r $IP2:$LOGS2 $LOGS_BASE/
  fi
}
# -------------- end of functions -----------------------------------------------------------------------------------------------


declare -r LOGS_BASE="/home/akka/log/$(date '+20%y-%m-%d--%H%M')"
declare -r IP1="$(head -n 1 /home/akka/multi-node-test.hosts)"
declare -r LOGS1="$LOGS_BASE/$IP1"

declare -r AERON_DIR=/dev/shm/aeron-akka

# -Daeron.threading.mode=DEDICATED -Daeron.sender.idle.strategy=org.agrona.concurrent.BusySpinIdleStrategy -Daeron.receiver.idle.strategy=org.agrona.concurrent.BusySpinIdleStrategy
declare -r MEDIA_DRIVER_ARGS="-Daeron.threading.mode=SHARED_NETWORK -Xms1g -Xmx1g -XX:+UseCompressedOops -XX:MaxDirectMemorySize=256m -XX:ReservedCodeCacheSize=256m -XX:BiasedLockingStartupDelay=0 -Daeron.mtu.length=16384 -Daeron.socket.so_sndbuf=2097152 -Daeron.socket.so_rcvbuf=2097152 -Daeron.rcv.initial.window.length=2097152 -Daeron.term.buffer.sparse.file=false -Dagrona.disable.bounds.checks=true"

declare -r MULTI_NODE_DIR="/home/akka/tmp-akka-multi-node"
declare -r MULTI_NODE_ARGS="-Dakka.test.multi-node=true -Dakka.test.multi-node.targetDirName=$MULTI_NODE_DIR -Dakka.remote.artery.advanced.flight-recorder.destination=target -Dmultinode.Xms1024M -Dmultinode.Xmx1024M -Dmultinode.XX:+PrintGCDetails -Dmultinode.XX:+PrintGCTimeStamps -Dmultinode.XX:BiasedLockingStartupDelay=0 -Dmultinode.Daeron.mtu.length=16384 -Dmultinode.Daeron.rcv.buffer.length=16384 -Dmultinode.Daeron.socket.so_sndbuf=2097152 -Dmultinode.Daeron.socket.so_rcvbuf=2097152 -Dmultinode.Daeron.rcv.initial.window.length=2097152 -Dmultinode.Dagrona.disable.bounds.checks=true -Dmultinode.XX:+UseCompressedOops -Dmultinode.XX:MaxDirectMemorySize=256m -Dmultinode.XX:+UnlockDiagnosticVMOptions -Dmultinode.XX:GuaranteedSafepointInterval=300000"
declare -r COMMON_ARGS="-Dakka.test.LatencySpec.totalMessagesFactor=10 -Dakka.test.LatencySpec.repeatCount=3 -Dakka.test.MaxThroughputSpec.totalMessagesFactor=200 -Dakka.remote.artery.advanced.embedded-media-driver=off -Dakka.remote.artery.advanced.aeron-dir=$AERON_DIR"
#declare -r COMMON_ARGS="-Dakka.remote.artery.advanced.embedded-media-driver=off -Dakka.remote.artery.advanced.aeron-dir=$AERON_DIR"

#declare -r ARGS="-Dakka.remote.artery.advanced.inbound-lanes=1"
declare -r ARGS="$1"
declare -r TEST="$2"

echo "logs in $LOGS_BASE"
mkdir -p $LOGS1

# description of test 
cd /home/akka/akka
echo "$RGS" > $LOGS_BASE/readme.txt
echo "MULTI_NODE_ARGS=$MULTI_NODE_ARGS" >> $LOGS_BASE/readme.txt
echo "COMMON_ARGS=$COMMON_ARGS" >> $LOGS_BASE/readme.txt
echo "MEDIA_DRIVER_ARGS=$MEDIA_DRIVER_ARGS" >> $LOGS_BASE/readme.txt
git log -n 1 >> $LOGS_BASE/readme.txt

# prepare all nodes
for ip in $(cat /home/akka/multi-node-test.hosts); do
  prepare $ip
done

sleep 10

# run the tests ------------------------------------------
echo "running test: $ARGS, log: $LOGS1/log.txt"
cd /home/akka/akka
cp /home/akka/multi-node-test.hosts /home/akka/akka/
/home/akka/sbt $COMMON_ARGS $MULTI_NODE_ARGS $ARGS -Dsbt.log.noformat=true "akka-remote-tests/multi-jvm:testOnly $TEST" > $LOGS1/log.txt
cat $LOGS1/log.txt | grep ===
cd /home/akka/


# cleanup ------------------------------------------------
for ip in $(cat /home/akka/multi-node-test.hosts); do
  finish $ip
done
