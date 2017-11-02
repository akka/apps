#!/bin/bash -x

# pull in the node ip addresses
. nodes-bash-exports.sh

for n in `seq 20 40`; do
  for d in `seq 1 10`; do
    curl 18.194.243.77:8080/introspector/pod-A-$n/write/data-$d 2> /dev/null > /dev/null
    curl 34.253.229.136:8080/introspector/pod-B-$n/write/data-$d 2> /dev/null > /dev/null
  done
done
