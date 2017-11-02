#!/bin/bash -x

export node_cassandra_central_1a_ip=52.59.221.33
export node_cassandra_central_1b_ip=54.93.225.114
export node_cassandra_central_1c_ip=35.158.104.198

export node_cassandra_west_1a_ip=34.240.4.26
export node_cassandra_west_1b_ip=176.34.145.253
export node_cassandra_west_1c_ip=54.154.113.241

export nodes_cassandra_all_ip=( "$node_cassandra_central_1a_ip" "$node_cassandra_central_1b_ip" "$node_cassandra_central_1c_ip" "$node_cassandra_west_1a_ip" "$node_cassandra_west_1b_ip" "$node_cassandra_west_1c_ip" )

export nodes_cassandra_central_ip=( "$node_cassandra_central_1a_ip" "$node_cassandra_central_1b_ip" "$node_cassandra_central_1c_ip" )

export nodes_cassandra_west_ip=( "$node_cassandra_west_1a_ip" "$node_cassandra_west_1b_ip" "$node_cassandra_west_1c_ip" )

