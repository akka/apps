#!/usr/bin/env bash

# Preparations:
#
# sudo sysctl net.core.rmem_max=2097152
# sudo sysctl net.core.wmem_max=2097152

declare -r IP1=0
declare -r IP2=1

declare -r LOGS_BASE="logs/$(date '+20%y-%m-%d--%H%M')"
declare -r LOGS1="$LOGS_BASE/$IP1"
declare -r LOGS2="$LOGS_BASE/$IP2"

declare -r AERON_DIR=/dev/shm/aeron

declare -r MULTI_NODE_DIR="$PWD/../tmp-akka-multi-node"
declare -r MULTI_NODE_ARGS="-Dakka.test.multi-node=true -Dakka.test.multi-node.targetDirName=$MULTI_NODE_DIR -Dmultinode.Xms1024M -Dmultinode.Xmx1024M -Dmultinode.XX:+PrintGCDetails -Dmultinode.XX:+PrintGCTimeStamps -Dmultinode.XX:BiasedLockingStartupDelay=0 -Dmultinode.Daeron.mtu.length=16384 -Dmultinode.Daeron.rcv.buffer.length=16384 -Dmultinode.Daeron.socket.so_sndbuf=2097152 -Dmultinode.Daeron.socket.so_rcvbuf=2097152 -Dmultinode.Daeron.rcv.initial.window.length=2097152 -Dmultinode.Dagrona.disable.bounds.checks=true -Dmultinode.XX:+UseCompressedOops -Dmultinode.XX:MaxDirectMemorySize=256m -Dmultinode.XX:+UnlockDiagnosticVMOptions -Dmultinode.XX:GuaranteedSafepointInterval=300000"
declare -r COMMON_ARGS="-Dakka.test.LatencySpec.totalMessagesFactor=30 -Dakka.remote.artery.advanced.embedded-media-driver=off -Dakka.remote.artery.advanced.aeron-dir=$AERON_DIR"

#declare -r ARGS="-Dakka.remote.artery.advanced.inbound-lanes=1"
declare -r ARGS="$4"

mkdir -p $LOGS1
ssh $IP2 "cd $PWD; mkdir -p $LOGS2"

# description of test
echo "$1" > $LOGS_BASE/readme.txt

#rm -rf $AERON_DIR
#ssh $IP2 "cd $PWD; rm -rf $AERON_DIR"

sbt 'akka-remote-tests/test:compile'
ssh $IP2 "cd $PWD; sbt 'akka-remote-tests/test:compile'"

sbt 'akka-remote/test:runMain io.aeron.driver.MediaDriver aeron.properties' &
ssh $IP2 "cd $PWD; sbt 'akka-remote/test:runMain io.aeron.driver.MediaDriver aeron.properties' &"
sleep 15

sbt -Daeron.dir=$AERON_DIR 'akka-remote/test:runMain akka.remote.artery.AeronStat type=[1-9]' > $LOGS1/aeron-stat.txt &
ssh $IP2 "cd $PWD; sbt -Daeron.dir=$AERON_DIR 'akka-remote/test:runMain akka.remote.artery.AeronStat type=[1-9]' > $LOGS2/aeron-stat.txt &

vmstat 1 > $LOGS1/vmstat.txt &
ssh $IP2 "cd $PWD; vmstat 1 > $LOGS2/vmstat.txt &"

sbt $MULTI_NODE_ARGS 'akka-remote-tests/multi-jvm:testOnly akka.remote.artery.LatencySpec' > $LOGS1/log.txt

cp -R $AERON_DIR $LOGS1
ssh $IP2 "cd $PWD; cp -R $AERON_DIR $LOGS2"

pkill vmstat
ssh $IP2 "cd $PWD; pkill vmstat"

pkill -f aeron
ssh $IP2 "cd $PWD; pkill -f aeron"

scp $IP2:$LOGS2/* $LOGS_BASE
