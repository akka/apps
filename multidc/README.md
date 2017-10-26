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

When doing a cross region cluster you can open up the ports in the security group but close them when you are done as
they are open to the world.

Run the chef on the new nodes e.g. 
* `fix nodes_with_role:re-cassandra`
* `fix nodes_with_role:re-akka`

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

## Running the tests

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
