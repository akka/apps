#!/usr/bin/env bash

# -------------- functions -------------------------------------------------------------------------------------------------------

# $1 - IP to execute scripts on
prepare() {
  declare -r IP2="$1"
  declare -r LOGS2="$LOGS_BASE/$IP2"

  echo "====== PREPARE $IP2 ======"
  
  cd /home/akka
  rsync -Pavuz /home/akka/apps akka@$IP2: > /dev/null
  rsync -Pavuz .ivy2 akka@$IP2: > /dev/null
  rsync -Pavuz .sbt akka@$IP2: > /dev/null
  
  ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $IP2 "
    cd $PWD

    rm -rf /dev/shm/aeron-*
    killall -9 java
    mkdir -p $LOGS2
  " &

}

# $1 - IP to execute scripts on
# $2 - class name to run
run() {
  declare -r IP2="$1"
  declare -r CLAZZ="$2"
  declare -r LOGS2="$LOGS_BASE/$IP2"

  echo "====== RUN $IP2 ======"
  
  ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $IP2 "
    cd $PWD

    mkdir -p $LOGS2

    cd /home/akka

    echo 'Running $CLAZZ on $IP2'
    cd /home/akka/apps
    /home/akka/sbt -J-Daeron.term.buffer.length=4194304 -Dsbt.log.noformat=true '; project sharding; runMain $CLAZZ' > $LOGS2/log-sharding.txt
  " &
      
  sleep 2
}

# $1 - IP to execute scripts on
finish() {
  declare -r IP2="$1"
  declare -r LOGS2="$LOGS_BASE/$IP2"

  echo "====== FINISH $IP2 ======"
  
  ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $IP2 "
    cd $PWD
  
    pkill -u akka java
    sleep 5
    pkill -9 -u akka java
  "
  
  # avoid copy own files
  if [ ! -d "$LOGS_BASE/$IP2" ]; then
    rsync -Pavuz $IP2:$LOGS2 $LOGS_BASE/
  fi
}
# -------------- end of functions -----------------------------------------------------------------------------------------------


declare -r LOGS_BASE="/home/akka/log/$(date '+20%y-%m-%d--%H%M')"
declare -r IP1="$(head -n 1 /home/akka/multi-node-test.hosts)"
declare -r LOGS1="$LOGS_BASE/$IP1"

declare -r ARGS="$1"

echo "logs in $LOGS_BASE"
mkdir -p $LOGS1

# description of test 
cd /home/akka/akka
git log -n 1 >> $LOGS_BASE/readme.txt

# prepare all nodes
for ip in $(cat /home/akka/multi-node-test.hosts); do
  prepare $ip
done

sleep 10

# run the tests ------------------------------------------
echo "running test...!"
for ip in $(cat /home/akka/multi-node-test.hosts); do
  run $ip "com.lightbend.akka.bench.sharding.latency.ShardingStartLatencyApp" 
done

echo "PRESS [ENTER] TO TERMINATE BENCHMARK"
read

# cleanup ------------------------------------------------
for ip in $(cat /home/akka/multi-node-test.hosts); do
  finish $ip
done
