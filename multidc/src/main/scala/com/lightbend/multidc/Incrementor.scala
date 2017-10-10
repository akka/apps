package com.lightbend.multidc

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.lightbend.multidc.ReplicatedCounter.{Increment, IncrementAck, ShardingEnvelope}

class Incrementor(id: String, nrIncrements: Int, clusterProxy: ActorRef) extends Actor with ActorLogging {
  import context._

  log.info("Incrementor for id {} to do {} increments", id, nrIncrements)

  clusterProxy ! ShardingEnvelope(id, Increment("do it"))

  def receive = toGo(nrIncrements - 1)

  def toGo(nr: Int): Receive = {
    case IncrementAck =>
      if (nr <= 0) {
        log.info("Incrementor for id {} finished", id)
        system.stop(self)
      } else {
        clusterProxy ! ShardingEnvelope(id, Increment("do it"))
        become(toGo(nr - 1))
      }
  }

}
