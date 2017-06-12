#!/usr/bin/env bash

if [ $# != "1" ]; then
  echo "Usage: . prepare-all.sh SUDO_PASS"
  exit 1
fi

PASS="$1"

for id in {0..8}
do
  node="moxie-a$id"
  echo
  echo "Deploying Chef to [$node]..."
  #  fix node:$node deploy_chef
  knife solo prepare --ssh-password "$PASS" local@$node
  echo
done

echo "Done!"