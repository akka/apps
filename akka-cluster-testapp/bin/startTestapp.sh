#!/bin/sh

# Use for seed nodes
#PORT="2552"
#LOG_Q="testapp-1"
#LOG_Q="testapp-2"
#LOG_Q="testapp-3"

# Use for other nodes
PORT="0"
LOG_Q="testapp-`uuidgen`"

HOST="a4.moxie"

SEED1="a0.moxie:2552"
SEED2="a2.moxie:2552"
SEED3="a3.moxie:2554"

LOG_DIR="logs"

AKKA_HOME="$(cd "$(cd "$(dirname "$0")"; pwd -P)"/..; pwd)"
AKKA_CLASSPATH="$AKKA_HOME/config:$AKKA_HOME/lib/*"

MEM_OPTS="-Xms128M -Xmx128M -XX:-HeapDumpOnOutOfMemoryError -XX:+UseParallelGC -XX:+UseCompressedOops"
PRINT_GC_OPTS="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
# Only one JMX (port) per machine
#JMX_OPTS="-Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
JMX_OPTS=""
HOST_OPTS="-Dakka.remote.netty.tcp.hostname=$HOST -Dakka.cluster.seed-nodes.1=akka.tcp://TestApp@$SEED1 -Dakka.cluster.seed-nodes.2=akka.tcp://TestApp@$SEED2 -Dakka.cluster.seed-nodes.3=akka.tcp://TestApp@$SEED3"
PORT_OPTS="-Dakka.remote.netty.tcp.port=$PORT"
LOG_OPTS="-Dtestapp.log-dir=$LOG_DIR -Dtestapp.log-qualifier=$LOG_Q"
JAVA_OPTS="$MEM_OPTS $PRINT_GC_OPTS $JMX_OPTS $HOST_OPTS $PORT_OPTS $LOG_OPTS"
JAVA_HOME="/usr/lib/jvm/java-7-openjdk-amd64"

mkdir -p $LOG_DIR

nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp "$AKKA_CLASSPATH" -Dakka.home="$AKKA_HOME" akka.kernel.Main testapp.Boot > $LOG_DIR/${HOST}_${LOG_Q}_out.txt 2>&1 &
