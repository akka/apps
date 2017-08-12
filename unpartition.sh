#!/usr/bin/env bash

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no akka-sharding-001 "
  sudo iptables -D INPUT -p udp -s akka-sharding-008 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-009 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-010 -j DROP
"

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no akka-sharding-002 "
  sudo iptables -D INPUT -p udp -s akka-sharding-008 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-009 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-010 -j DROP
"

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no akka-sharding-003 "
  sudo iptables -D INPUT -p udp -s akka-sharding-008 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-009 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-010 -j DROP
"

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no akka-sharding-004 "
  sudo iptables -D INPUT -p udp -s akka-sharding-008 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-009 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-010 -j DROP
"

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no akka-sharding-005 "
  sudo iptables -D INPUT -p udp -s akka-sharding-008 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-009 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-010 -j DROP
"

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no akka-sharding-006 "
  sudo iptables -D INPUT -p udp -s akka-sharding-008 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-009 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-010 -j DROP
"

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no akka-sharding-007 "
  sudo iptables -D INPUT -p udp -s akka-sharding-008 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-009 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-010 -j DROP
"


ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no akka-sharding-008 "
  sudo iptables -D INPUT -p udp -s akka-sharding-001 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-002 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-003 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-004 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-005 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-006 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-007 -j DROP
"

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no akka-sharding-009 "
  sudo iptables -D INPUT -p udp -s akka-sharding-001 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-002 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-003 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-004 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-005 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-006 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-007 -j DROP
"

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no akka-sharding-010 "
  sudo iptables -D INPUT -p udp -s akka-sharding-001 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-002 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-003 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-004 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-005 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-006 -j DROP
  sudo iptables -D INPUT -p udp -s akka-sharding-007 -j DROP
"
