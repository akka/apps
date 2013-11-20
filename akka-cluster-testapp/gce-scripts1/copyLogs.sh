#!/bin/sh

DIR=test10

mkdir /mnt/akka-logs/$DIR

for ((j=2; j<=1500; j++)); do N=`printf "%04d\n" "$j"`; scp -o StrictHostKeyChecking=no n$N:/home/patrik/akka-logs/n* /mnt/akka-logs/$DIR/; done

cp /mnt/akka-logs/n* /mnt/akka-logs/$DIR/

#find /mnt/akka-logs/$DIR/*out.txt | xargs grep 'OutOfMemory' -sl

#sudo tcpdump -i eth0 -s 65535 -w /mnt/akka-logs/$DIR/tcpdump1.log

#gzip /mnt/akka-logs/$DIR/*
