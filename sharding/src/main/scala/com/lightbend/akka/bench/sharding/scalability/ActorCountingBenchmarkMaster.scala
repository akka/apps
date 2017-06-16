package com.lightbend.akka.bench.sharding.scalability

import java.util
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.cluster.Cluster

class ActorCountingBenchmarkMaster extends Actor with ActorLogging {
  val config = context.system.settings.config
  val addActorsBatch = config.getInt("shard-bench.add-actors-batch")
  val addActorsInterval = config.getDuration("shard-bench.add-actors-interval", TimeUnit.MILLISECONDS).millis
  
  val cluster = Cluster(context.system)
  
  // number of actors we sent a message to (so they should start in sharding)
  var totalActorsInSharding = 0
  // number of actors who have not yet replied back that they've started
  var pendingAliveConfirmation = 0
  var confirmations = 0
  val timesWhenWeStartedBatch = new util.ArrayDeque[Long](10) 
  
  override def preStart(): Unit = {
    context.system.scheduler.schedule(addActorsInterval, addActorsInterval, self, AddMoreActors)
  }
  
  val sharding: ActorRef = ActorCountingEntity.proxy(context.system)
  
  override def receive: Receive = {
    case ActorCountingEntity.Ready(startTime) =>
      
      // not really goal of this benchmark though: 
      // if (log.isDebugEnabled)
      //   log.debug("Actor {} took [{} ns] to initialize and reply-back", sender().path.name, startTime - System.nanoTime())
      
      pendingAliveConfirmation -= 1
      confirmations += 1
      if (confirmations % addActorsBatch == 0) {
        val timeToStartBatch = (System.nanoTime() - timesWhenWeStartedBatch.pop()).nanos.toMillis
        log.info("FINISHED INITIALIZING ANOTHER {} ACTORS IN [{} ms]!!!", addActorsBatch, timeToStartBatch)
      }
      
    case AddMoreActors =>
      log.info(
        s"Adding:+${addActorsBatch} actors. " +
          s"Total actors before:[${totalActorsInSharding}]. " +
          s"STILL pending alive-confirmation:[${pendingAliveConfirmation}] (so ${, pendingAliveConfirmation + addActorsBatch} now). " +
          s"At cluster size:${cluster.state.members.size} nodes)") 
      
      timesWhenWeStartedBatch.addLast(System.nanoTime())
      
      var i = 0 
      while (i < addActorsBatch) {
        totalActorsInSharding += 1 
        sharding ! ActorCountingEntity.Start(totalActorsInSharding, System.nanoTime())
        i += 1
      }
      pendingAliveConfirmation += addActorsBatch
  }
}

final case object AddMoreActors
