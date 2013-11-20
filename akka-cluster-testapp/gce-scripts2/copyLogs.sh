#!/bin/sh

DIR=test08

sudo mkdir /mnt/akka-logs
sudo /usr/share/google/safe_format_and_mount -m "mkfs.ext4 -F" /dev/disk/by-id/google-akka-logs /mnt/akka-logs

mkdir /mnt/akka-logs/$DIR

for ((j=2; j<=1500; j++)); do N=`printf "%04d\n" "$j"`; scp -o StrictHostKeyChecking=no n$N:/tmp/akka-logs/n* /mnt/akka-logs/$DIR/; done

cp /mnt/akka-logs/n* /mnt/akka-logs/$DIR/

#find /mnt/akka-logs/$DIR/*out.txt | xargs grep 'OutOfMemory' -sl

#sudo tcpdump -i eth0 -s 65535 -w /mnt/akka-logs/$DIR/tcpdump1.log

#gzip /mnt/akka-logs/$DIR/*
