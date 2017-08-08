#!/usr/bin/env bash

cd /home/akka/akka
git checkout wip-gcloud-faninout-2m

declare -r TEST="$1"

# cpu-level

/home/akka/run-multinode-benchmark.sh "-Dtest=cpu1-a -Dakka.remote.artery.advanced.idle-cpu-level=5 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST
/home/akka/run-multinode-benchmark.sh "-Dtest=cpu1-b -Dakka.remote.artery.advanced.idle-cpu-level=5 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST

#/home/akka/run-multinode-benchmark.sh "-Dtest=012-a -Dakka.remote.artery.advanced.idle-cpu-level=3 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST
#/home/akka/run-multinode-benchmark.sh "-Dtest=012-b -Dakka.remote.artery.advanced.idle-cpu-level=3 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST

#/home/akka/run-multinode-benchmark.sh "-Dtest=013-a -Dakka.remote.artery.advanced.idle-cpu-level=7 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST
#/home/akka/run-multinode-benchmark.sh "-Dtest=013-b -Dakka.remote.artery.advanced.idle-cpu-level=7 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST

#/home/akka/run-multinode-benchmark.sh "-Dtest=013-a -Dakka.remote.artery.advanced.idle-cpu-level=9 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST
#/home/akka/run-multinode-benchmark.sh "-Dtest=013-b -Dakka.remote.artery.advanced.idle-cpu-level=9 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST

#/home/akka/run-multinode-benchmark.sh "-Dtest=014-a -Dakka.remote.artery.advanced.idle-cpu-level=10 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST
#/home/akka/run-multinode-benchmark.sh "-Dtest=014-b -Dakka.remote.artery.advanced.idle-cpu-level=10 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST

#/home/akka/run-multinode-benchmark.sh "-Dtest=015-a -Dakka.remote.artery.advanced.idle-cpu-level=10 -Daeron.threading.mode=DEDICATED -Daeron.sender.idle.strategy=org.agrona.concurrent.BusySpinIdleStrategy -Daeron.receiver.idle.strategy=org.agrona.concurrent.BusySpinIdleStrategy -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST
#/home/akka/run-multinode-benchmark.sh "-Dtest=015-b -Dakka.remote.artery.advanced.idle-cpu-level=10 -Daeron.threading.mode=DEDICATED -Daeron.sender.idle.strategy=org.agrona.concurrent.BusySpinIdleStrategy -Daeron.receiver.idle.strategy=org.agrona.concurrent.BusySpinIdleStrategy -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST

# real-message

#/home/akka/run-multinode-benchmark.sh "-Dtest=021-a -Dakka.test.MaxThroughputSpec.real-message=on -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST
#/home/akka/run-multinode-benchmark.sh "-Dtest=021-b -Dakka.test.MaxThroughputSpec.real-message=on -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST

# default

#/home/akka/run-multinode-benchmark.sh "-Dtest=031-a -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST
#/home/akka/run-multinode-benchmark.sh "-Dtest=031-b -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST

# no flight-recorder

#/home/akka/run-multinode-benchmark.sh "-Dtest=041-a -Dakka.remote.artery.advanced.iflight-recorder.enabled=off -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST
#/home/akka/run-multinode-benchmark.sh "-Dtest=041-b -Dakka.remote.artery.advanced.iflight-recorder.enabled=off -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST

# term buffer, default is 16 * 1024 * 1024 = 16777216

# term buffer 1/4 of default
#/home/akka/run-multinode-benchmark.sh "-Dtest=051-a -Daeron.term.buffer.length=4194304 -Daeron.term.buffer.sparse.file=true -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST
#/home/akka/run-multinode-benchmark.sh "-Dtest=051-b -Daeron.term.buffer.length=4194304 -Daeron.term.buffer.sparse.file=true -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST

# term buffer min value
#/home/akka/run-multinode-benchmark.sh "-Dtest=052-a -Daeron.term.buffer.length=65536 -Daeron.term.buffer.sparse.file=true -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST
#/home/akka/run-multinode-benchmark.sh "-Dtest=052-a -Daeron.term.buffer.length=65536 -Daeron.term.buffer.sparse.file=true -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4" $TEST

cd /home/akka

zip -r "/home/akka/log/logs-$(date '+20%y-%m-%d--%H%M').zip" /home/akka/log/* -x "*.logbuffer" -x "*.zip"
