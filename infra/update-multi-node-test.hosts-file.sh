#!/bin/sh

# examples:
#
# ./update-multi-node-test.hosts-file.sh akka-shard

if [[ "$#" != "1" ]]; then
  echo "USAGE: ./make-node-files-from-gcloud.sh GREP SEED_NODES"
  echo "EXTRA PARAMS:"
  echo "  you can set: EXTRA_RUNLIST (   example: EXTRA_RUNLIST='"role[benchmark-ddata]",'   )"
fi

declare -r GREP=$1

declare -r nodes=$(gcloud compute instances list | grep $GREP | wc -l | awk '{print $4}')

# cassandra contact points as json (just the first ... nodes)
declare -r CASSANDRA_CONTACT_POINTS_NUM=2
declare -r CASSANDRA_CONTACT_POINTS=$(
  gcloud compute instances list |
    grep "cassandra" | grep RUNNING |
    awk ' {print $4 } ' |
    head -n${CASSANDRA_CONTACT_POINTS_NUM} | 
    awk ' BEGIN { ORS = ""; print "["; } { print "\/\@"$0"\/\@"; } END { print "]"; }' | sed "s^\"^\\\\\"^g;s^\/\@\/\@^\", \"^g;s^\/\@^\"^g" 
  )

gcloud compute instances list | grep akka-shard | awk '{ print $4 }'
