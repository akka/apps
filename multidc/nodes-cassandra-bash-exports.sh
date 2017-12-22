#!/bin/bash -x

export node_cassandra_central_1a_ip=35.158.122.93
export node_cassandra_central_1b_ip=54.93.225.104
export node_cassandra_central_1c_ip=18.195.21.84

export node_cassandra_west_1a_ip=54.194.79.5
export node_cassandra_west_1b_ip=34.242.227.97
export node_cassandra_west_1c_ip=34.242.248.117

export nodes_cassandra_all_ip=( "$node_cassandra_central_1a_ip" "$node_cassandra_central_1b_ip" "$node_cassandra_central_1c_ip" "$node_cassandra_west_1a_ip" "$node_cassandra_west_1b_ip" "$node_cassandra_west_1c_ip" )

export nodes_cassandra_central_ip=( "$node_cassandra_central_1a_ip" "$node_cassandra_central_1b_ip" "$node_cassandra_central_1c_ip" )

export nodes_cassandra_west_ip=( "$node_cassandra_west_1a_ip" "$node_cassandra_west_1b_ip" "$node_cassandra_west_1c_ip" )

