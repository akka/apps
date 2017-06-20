#!/usr/bin/env bash

cd /home/akka/akka
git checkout wip-gcloud-bench-patriknw

# LatencySpec
git checkout ae8003a

# inbound-lanes

/home/akka/run-multinode-benchmark.sh "-Dtest=101-a -Dakka.remote.artery.advanced.inbound-lanes=1"

/home/akka/run-multinode-benchmark.sh "-Dtest=102-a -Dakka.remote.artery.advanced.inbound-lanes=4"

# cpu-level

/home/akka/run-multinode-benchmark.sh "-Dtest=111-a -Dakka.remote.artery.advanced.idle-cpu-level=1 -Dakka.remote.artery.advanced.inbound-lanes=1"

/home/akka/run-multinode-benchmark.sh "-Dtest=112-a -Dakka.remote.artery.advanced.idle-cpu-level=3 -Dakka.remote.artery.advanced.inbound-lanes=1"

/home/akka/run-multinode-benchmark.sh "-Dtest=113-a -Dakka.remote.artery.advanced.idle-cpu-level=7 -Dakka.remote.artery.advanced.inbound-lanes=1"

/home/akka/run-multinode-benchmark.sh "-Dtest=113-a -Dakka.remote.artery.advanced.idle-cpu-level=9 -Dakka.remote.artery.advanced.inbound-lanes=1"

/home/akka/run-multinode-benchmark.sh "-Dtest=114-a -Dakka.remote.artery.advanced.idle-cpu-level=10 -Dakka.remote.artery.advanced.inbound-lanes=1"

/home/akka/run-multinode-benchmark.sh "-Dtest=115-a -Dakka.remote.artery.advanced.idle-cpu-level=10 -Daeron.threading.mode=DEDICATED -Daeron.sender.idle.strategy=org.agrona.concurrent.BusySpinIdleStrategy -Daeron.receiver.idle.strategy=org.agrona.concurrent.BusySpinIdleStrategy -Dakka.remote.artery.advanced.inbound-lanes=1"

/home/akka/run-multinode-benchmark.sh "-Dtest=116-a -Dakka.remote.artery.advanced.idle-cpu-level=10 -Daeron.threading.mode=DEDICATED -Daeron.sender.idle.strategy=org.agrona.concurrent.BusySpinIdleStrategy -Daeron.receiver.idle.strategy=org.agrona.concurrent.BusySpinIdleStrategy -Dakka.remote.artery.advanced.inbound-lanes=4"

# real-message

/home/akka/run-multinode-benchmark.sh "-Dtest=121-a -Dakka.test.LatencySpec.real-message=on -Dakka.remote.artery.advanced.inbound-lanes=1"

/home/akka/run-multinode-benchmark.sh "-Dtest=122-a -Dakka.test.LatencySpec.real-message=on -Dakka.remote.artery.advanced.inbound-lanes=4"

# dispatcher

/home/akka/run-multinode-benchmark.sh "-Dtest=131-a -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=4 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=4 -Dakka.remote.artery.advanced.inbound-lanes=4"

/home/akka/run-multinode-benchmark.sh "-Dtest=132-a -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min=6 -Dakka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max=6 -Dakka.remote.artery.advanced.inbound-lanes=4"


# flight-recorder

/home/akka/run-multinode-benchmark.sh "-Dtest=141-a -Dakka.remote.artery.advanced.iflight-recorder.enabled=off -Dakka.remote.artery.advanced.inbound-lanes=4"

# TODO embedded media driver

cd /home/akka/akka
git checkout wip-gcloud-bench-patriknw
cd /home/akka

zip -r "/home/akka/log/logs-$(date '+20%y-%m-%d--%H%M').zip" /home/akka/log/* -x "*.logbuffer" -x "*.zip"
