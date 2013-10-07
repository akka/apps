#!/bin/sh

AKKA_HOME="$(cd "$(cd "$(dirname "$0")"; pwd -P)"/..; pwd)"


runRepeat() {
  #for i in {1..3}; do
  for i in $(seq 1 10); do	
    ${AKKA_HOME}/bin/startTestapp.sh
    
  done
}

runRepeat

echo "Total `pgrep -f Dtestapp|wc -l` testapps running, kill all with 'pkill -f Dtestapp'"

