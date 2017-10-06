#!/usr/bin/env bash

aws ec2 describe-instances --query 'Reservations[].Instances[].[PublicIpAddress,Tags[?Key==`Name`].Value[]]' --output text | sed '$!N;s/\n/ /' | grep re