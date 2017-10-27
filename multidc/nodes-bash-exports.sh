#!/bin/bash -x

export node_central_1a="re-akka-eucentral-1a"
export node_central_1a_ip=$(dig "$node_central_1a" | awk '/^;; ANSWER SECTION:$/ { getline ; print $5 }')
export node_central_1b="re-akka-eucentral-1b"
export node_central_1b_ip=$(dig "$node_central_1b" | awk '/^;; ANSWER SECTION:$/ { getline ; print $5 }')
export node_central_1c="re-akka-eucentral-1c"
export node_central_1c_ip=$(dig "$node_central_1c" | awk '/^;; ANSWER SECTION:$/ { getline ; print $5 }')

export node_west_1a="re-akka-euwest-1a"
export node_west_1a_ip=$(dig "$node_west_1a" | awk '/^;; ANSWER SECTION:$/ { getline ; print $5 }')
export node_west_1b="re-akka-euwest-1b"
export node_west_1b_ip=$(dig "$node_west_1b" | awk '/^;; ANSWER SECTION:$/ { getline ; print $5 }')
export node_west_1c="re-akka-euwest-1c"
export node_west_1c_ip=$(dig "$node_west_1c" | awk '/^;; ANSWER SECTION:$/ { getline ; print $5 }')

export nodes_all=( "$node_central_1a" "$node_central_1b" "$node_central_1c" "$node_west_1a" "$node_west_1b" "$node_west_1c" )
export nodes_all_ip=( "$node_central_1a_ip" "$node_central_1b_ip" "$node_central_1c_ip" "$node_west_1a_ip" "$node_west_1b_ip" "$node_west_1c_ip" )

export nodes_central=( "$node_central_1a" "$node_central_1b" "$node_central_1c" )
export nodes_central_ip=( "$node_central_1a_ip" "$node_central_1b_ip" "$node_central_1c_ip" )

export nodes_west=( "$node_west_1a" "$node_west_1b" "$node_west_1c" )
export nodes_west_ip=( "$node_west_1a_ip" "$node_west_1b_ip" "$node_west_1c_ip" )

