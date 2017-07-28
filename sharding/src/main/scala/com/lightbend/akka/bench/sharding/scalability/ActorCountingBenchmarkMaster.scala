package com.lightbend.akka.bench.sharding.scalability

import java.util

import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.cluster.Cluster
import com.lightbend.akka.bench.sharding.BenchSettings

import scala.concurrent.duration._

class ActorCountingBenchmarkMaster extends Actor with ActorLogging {
  implicit val dispatcher = context.system.dispatcher
  val settings = BenchSettings(context.system)
  val addActorsBatch: Int = settings.AddActorsPerBatch
  val addActorsInterval: FiniteDuration = settings.AddActorsInterval
  
  val cluster = Cluster(context.system)
  
  // number of actors we sent a message to (so they should start in sharding)
  var totalStartingActorsInSharding = 0L
  // number of actors who have not yet replied back that they've started
  var pendingAliveConfirmation = 0L
  var totalAliveConfirmedActors = 0L
  val timesWhenWeStartedBatch = new util.ArrayDeque[Long](10) 
  
  override def preStart(): Unit = {
    context.system.scheduler.schedule(0.seconds, addActorsInterval, self, AddMoreActors)
  }
  
  val sharding: ActorRef = ActorCountingEntity.proxy(context.system)
  
  override def receive: Receive = {
    case ActorCountingEntity.Ready(startTime) =>
      // not really goal of this benchmark though: 
      // if (log.isDebugEnabled)
      //   log.debug("Actor {} took [{} ns] to initialize and reply-back", sender().path.name, startTime - System.nanoTime())
      
      pendingAliveConfirmation -= 1
      totalAliveConfirmedActors += 1
      if (totalAliveConfirmedActors % addActorsBatch == 0) {
        val timeToStartBatch = (System.nanoTime() - timesWhenWeStartedBatch.pop()).nanos.toMillis
        log.info("FINISHED INITIALIZING ANOTHER {} ACTORS (TOTAL: {}) IN [{} ms]!!!", addActorsBatch, totalAliveConfirmedActors, timeToStartBatch)
      }
      
    case AddMoreActors =>
      log.info(
        s"Adding:+${addActorsBatch} actors. " +
          s"Total actors before:[${totalStartingActorsInSharding}]. " +
          s"STILL pending alive-confirmation:[${pendingAliveConfirmation}] (so ${pendingAliveConfirmation + addActorsBatch} now). " +
          s"At cluster size:${cluster.state.members.size} nodes)") 
      
      timesWhenWeStartedBatch.addLast(System.nanoTime())
      
      var i = 0 
      while (i < addActorsBatch) {
        totalStartingActorsInSharding += 1 
        sharding ! ActorCountingEntity.Start(totalStartingActorsInSharding, System.nanoTime())
        i += 1
      }
      pendingAliveConfirmation += addActorsBatch
  }
}

final case object AddMoreActors
