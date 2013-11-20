#! /bin/bash

# only on n0001
sudo mkdir /mnt/akka-logs
sudo /usr/share/google/safe_format_and_mount -m "mkfs.ext4 -F" /dev/disk/by-id/google-akka-logs /mnt/akka-logs

sudo mkdir /mnt/akka-testapp
sudo /usr/share/google/safe_format_and_mount -m "mkfs.ext4 -F" /dev/disk/by-id/google-akka-testapp /mnt/akka-testapp

sudo tar -C /opt -xf /mnt/akka-testapp/install/jdk-7u40-linux-x64.tar

cd /mnt/akka-testapp/
bin/startTestappSeed1.sh


