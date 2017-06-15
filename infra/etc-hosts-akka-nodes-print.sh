#!/bin/sh

gcloud compute instances list | grep akka-node | awk ' { print $5," ",$1 } '
