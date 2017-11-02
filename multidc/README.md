# Multi DC tests

Running multi dc tests has three stages, automating to varying degrees:

* This app
* AWS infra
* Running the test

## AWS infra

Creation of the hosts is not automated (yet).

There exists akka nodes and cassandra nodes in ireland and frankfurt regions that can be cloned to expand the clsuter.

After cloning update the node tag:
* e.g. re-akka-eucentral-1a update the number and have the letter be the availability zone, this needs to be unique

Use `infra/make-re-nodes-from-aws.sh` to generate the chef node files. It relies on tags so if you create
new nodes without copying then add the role, name and purpose tags.

I think if we add another region or want bigger clusters we should automate the creation of the nodes with chef or cli/shell.

The nodes currently have SSH access from my IP, change the security group to yours.

When doing a cross region cluster you can open up the ports in the security group but close them when you are done as they are open to the world.

Run the chef on the new nodes e.g. 
* `fix nodes_with_role:cassandra-re`
* `fix nodes_with_role:akka-re`

This requires passwordless ssh access. The keys re in your inbox and your ssh config will look something like this:

```
Host re-cassandra-euwest-1a re-cassandra-euwest-1b re-cassandra-euwest-1c re-akka-euwest-1a re-akka-euwest-1b re-akka-euwest-1c
     User ubuntu
     Port 22
     IdentityFile ~/.ssh/replicated-entity.pem

Host re-cassandra-eucentral-1a re-cassandra-eucentral-1b re-cassandra-eucentral-1c re-akka-eucentral-1a re-akka-eucentral-1b re-akka-eucentral-1c
     User ubuntu
     Port 22
     IdentityFile ~/.ssh/re-central.pem

```

If you shut down the nodes (do that they are expensive) then AWS will give them new IPs so you'll need to re-do the above steps.

## Running the tests (counter)

The chef puts the project in /home/akka/multidc/apps

I used tmux synchronise panes to start up sbt consoles and run the cluster app. This could be improved.

Hit the HTTP API e.g.

```
 curl -v "localhost:8080/test?counters=1&updates=1"
```

To up date one counter one time.

```
curl -v "localhost:8080/counter?id=0"
```

To get the value for a counter.

## Running the "introspector"

This way of testing lets you manually do single writes on either side of the cluster.

There are utilities for splitting and healing partitions, of cassandra as well as the akka cluster itself.

Workflow is basically:
- start all nodes
- make sure to have `replicated-entity.pem`, it's in the team keybase
go to infra (and the `re-central.pem`)
  - make-re-node-files-from-aws.sh -- refreshes all the IPs and seed nodes in local files
- make sure to run it for both regions, eu-central-1 and eu-west-1 for example (edit the file and run it again)
- `fix` all nodes
  - `fix nodes_with_role:cassandra-re` -- actually updates the servers with the above
  - `fix nodes_with_role:akka-re` -- actually updates the servers with the above
- all this was from infra, now move back to multidc and from here you can develop and experiment
run the app on all nodes run-multi-dc-test-remote.sh
- IPs are automatically obtained from nodes-bash-exports.sh, though it should be done a bit nicer perhaps later on with using aws command line
  - you also need to add IPs in the `nodes-bash-exports...` files (!)
  
Running and experimenting with things:
- if you want to experiment, use `rsync-local-src-to-akka-nodes.sh` to sync the sources (src) to all nodes
- then kill all nodes `kill-multi-dc-test-remote-run.sh` and start them again
- if you want to introduce package drops use network-fleaky-node.sh, **which is not complete yet** 
- network splits
  - `network-split-remoting.sh` or `network-split-cassandra.sh` is used for making a split; do look into the code and edit to get exaclty what you want. 
  - Syntax is `network-split-cassandra.sh split` or `network-split-cassandra.sh heal`
  - Syntax is `network-split-remoting.sh split` or `network-split-remoting.sh heal`
  
Do look into the scripts to know what they are doing.

### Inspecting and doing writes

You'll see all clustered nodes logs in the console where you did `run...`.

You can also use akka-cluster management: it's exposed on each node under `http ...:19999/...`, so you can GET the `/members` for example.

Use JMX to look into cassandra writes.

perform writes by sending:

```
$ http IP_HERE:8080/introspector/alpha/write/DATA_TO_WRITE
```

this returns the current `state` with detailed information.

You can just inspect the state via:

```
$ http IP_HERE:8080/introspector/alpha/
```




## Cassandra cluster

I use tmux synchronize panes to run a `nodetool status` to see each nodes view of the cluster. Ensure they are all UN before
starting any test.

### Metrics

Open up JMX ports to your IP to attach visualvm/jconsole to get stats. There is a bajillion stats that cassandra exports via JMX.
I suggest looking at ClientRequest at first, to understand what they are see [shameless plug](http://batey.info/cassandra-clientrequest-metrics.html)


## Improvements

* Automate node creation
* Add route 53 entires / a domain so IPs dont matter.
* More than one seed for cassandra and akka clusters
