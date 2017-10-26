package com.lightbend.multidc

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.lightbend.multidc.ReplicatedCounter.{Increment, IncrementAck, ShardingEnvelope}

import scala.concurrent.duration.Duration

class Incrementor(id: String, nrIncrements: Int, clusterProxy: ActorRef) extends Actor with ActorLogging {
  import context._

  val startTime = System.currentTimeMillis()

  log.info("Incrementor for id {} to do {} increments", id, nrIncrements)

  clusterProxy ! ShardingEnvelope(id, Increment("do it"))

  def receive = toGo(nrIncrements - 1)

  def toGo(nr: Int): Receive = {
    case IncrementAck =>
      if (nr <= 0) {
        log.info("Incrementor for id {} finished. Took {} seconds", id, Duration(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS).toSeconds)
        system.stop(self)
      } else {
        clusterProxy ! ShardingEnvelope(id, Increment("do it"))
        become(toGo(nr - 1))
      }
  }

}
