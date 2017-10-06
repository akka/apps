#!/bin/bash -x

DC1=localhost:8080
DC2=localhost:8081

curl -v "$DC1/members" | jq

curl -v "$DC1/single-counter-test?counter=first&updates=100"

#curl -v "$DC1/single-counter-test?counter=first&updates=100"

curl -v "$DC1/counter?id=first"

curl -v "$DC1/test?counters=100&updates=100"
curl -v "$DC1/counter?id=99"


