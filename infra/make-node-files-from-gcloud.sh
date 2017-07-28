#!/bin/sh

# examples:
#
# ./make-node-files-from-gcloud.sh akka-node-01 '["10.154.0.2"]'
#
# EXTRA_RUNLIST='"role[benchmark-sharding]",' ./make-node-files-from-gcloud.sh akka-node-00 '["10.132.0.7", "10.132.0.8"]'


# -------------------- functions --------------------  
json_append() {
  local old_json=$1; shift
  local count=0
  local -a args=( )
  local elem_names=''
  while (( $# )); do
    args+=( --arg "element$((++count))" "$1" )
    elem_names+="\$element$count, "
    shift
  done

  jq "${args[@]}" ". + [ ${elem_names%, } ]" <<<"${old_json:-[]}"
}
# ----------------- end of functions -----------------  

if [[ "$#" != "2" ]]; then
  echo "USAGE: ./make-node-files-from-gcloud.sh GREP SEED_NODES"
  echo "EXTRA PARAMS:"
  echo "  you can set: EXTRA_RUNLIST (   example: EXTRA_RUNLIST='"role[benchmark-ddata]",'   )"
fi

declare -r GREP=$1
declare -r SEED_NODES=$2

declare -r minumum_nodes_NUM=$(gcloud compute instances list | grep $GREP | wc -l | awk '{print $1}')

# cassandra contact points as json (just the first ... nodes)
declare -r CASSANDRA_CONTACT_POINTS_NUM=2
declare -r CASSANDRA_CONTACT_POINTS=$(
  gcloud compute instances list |
    grep "cassandra" | grep RUNNING |
    awk ' {print $4 } ' |
    head -n${CASSANDRA_CONTACT_POINTS_NUM} | 
    awk ' BEGIN { ORS = ""; print "["; } { print "\/\@"$0"\/\@"; } END { print "]"; }' | sed "s^\"^\\\\\"^g;s^\/\@\/\@^\", \"^g;s^\/\@^\"^g" 
  )

IFS=$'\n'       # make newlines the only separator
for node in $(gcloud compute instances list | grep "$GREP" | grep "RUNNING" | awk ' { print $1,$4,$5 } ')
do
  name=$(echo $node | awk ' { print $1} ')
  internal_ip=$(echo $node | awk ' { print $2} ')
  external_ip=$(echo $node | awk ' { print $3} ')
  
  echo 
  
  cat ./nodes/akka-node.json.template |
    sed "s/NAME/$name/g" |
    sed "s/INTERNAL_IP/$internal_ip/g" |
    sed "s/EXTERNAL_IP/$external_ip/g" |
    sed "s/AKKA_SEED_NODES/$SEED_NODES/g" |
    sed "s/EXTRA_RUNLIST/$EXTRA_RUNLIST/g" |
    sed "s/CASSANDRA_CONTACT_POINTS/$CASSANDRA_CONTACT_POINTS/g" |
    sed "s/minumum_nodes/$minumum_nodes_NUM/g"  > ./nodes/$name.json 
    
    echo "Generated: ./nodes/$name.json" 
done
