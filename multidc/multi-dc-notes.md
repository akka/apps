2DCs
3 C* nodes per DC
3 Akka nodes per DC

1000 updates to a single counter
time curl -v "localhost:8080/single-counter-test?counter=chris&updates=1000"

Replicated successfully to both DCs 

Produces 2k writes as expected:

```
cqlsh:akka> select count(*) from messages;

 count
-------
  2000

(1 rows)
```

Latency on the read table (single partition read is negligible):

```
ubuntu@ip-172-31-45-92:~$ nodetool cfhistograms  akka messages_notification                                
No SSTables exists, unable to calculate 'Partition Size' and 'Cell Count' percentiles                      
akka/messages_notification histograms                
Percentile  SSTables     Write Latency      Read Latency    Partition Size        Cell Count               
                              (micros)          (micros)           (bytes)                                 
50%             0.00             17.08             35.43               NaN               NaN               
75%             0.00             20.50             42.51               NaN               NaN               
95%             0.00             20.50             88.15               NaN               NaN               
98%             0.00             20.50            105.78               NaN               NaN               
99%             0.00             20.50            105.78               NaN               NaN               
Min             0.00             11.87              5.72               NaN               NaN               
Max             0.00             20.50            315.85               NaN               NaN             
```

Second 1000 writes to the same counter, monitoring cfstats on a single node:

Before:

```
Keyspace: akka
        Read Count: 10324198
        Read Latency: 0.25709201131167764 ms.
        Write Count: 8857654
        Write Latency: 0.03076243427435752 ms.
        Pending Flushes: 0

```

After:

```
Keyspace: akka
        Read Count: 10326317
        Read Latency: 0.257049715789279 ms.
        Write Count: 8859665
        Write Latency: 0.030769769624472258 ms.
```

2011 writes. Most to the messages table and a few to the notifications table.

2119 reads. Hrmmmmm. 

Messages table:

Before:
```
  Local read count: 10258913
  Local read latency: 0.028 ms
  Local write count: 8849462
  Local write latency: 0.020 ms
```

After:
```
Local read count: 10260950
Local read latency: 0.061 ms
Local write count: 8851466
Local write latency: 0.094 ms

```

So nearly all on the messages table. This must be rows read.

Attaching JConsole to a Cassandra node:

Running 3000 counters updated in 1 DC 2000 times, acking each increment before doing the next.

Cassandra writes settle at 2.5/node.

Reads end up ~1500/s per node for 3000 active entities in one DC.

This was a bug. Reads down to about 100/node.

Next test was doing the same 3k entities in both DCs and incrementing them all 1k times. 

This causes a lot of:

```
java.lang.IllegalStateException: Missing sequence number [3942], got [3943] for persistenceId [counter|2057|eu-central].                                                                                              
        at akka.persistence.cassandra.query.EventsByPersistenceIdStage$$anon$1.akka$persistence$cassandra$query$EventsByPersistenceIdStage$$anon$$tryPushOne(EventsByPersistenceIdStage.scala:323)                    
        at akka.persistence.cassandra.query.EventsByPersistenceIdStage$$anon$1.onPull(EventsByPersistenceIdStage.scala:280) 
```

But it all recovers without intervention and all counters ended up up to date.

Next, switch to artery and repeat.
