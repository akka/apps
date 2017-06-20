#!/bin/sh

declare -r GREP=$1

gcloud compute instances list | grep "$GREP" | grep RUNNING | awk ' { print $5," ",$1 } '
