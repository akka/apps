/*
 * Copyright 2017 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightbend.akka.bench.sharding.scalability

import java.util

import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.cluster.Cluster
import akka.event.Logging
import akka.stream.Attributes
import akka.stream.scaladsl.{ Flow, Source }
import com.lightbend.akka.bench.sharding.BenchSettings

import scala.concurrent.duration._
import akka.actor.Props

object ActorCountingBenchmarkMaster {
  def props(sharding: ActorRef): Props =
    Props(new ActorCountingBenchmarkMaster(sharding))
}

class ActorCountingBenchmarkMaster(sharding: ActorRef) extends Actor with ActorLogging {
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
  var batchCount = 0
  var timesWhenWeStartedBatch = Map.empty[Int, Long]

  override def preStart(): Unit = {
    context.system.scheduler.schedule(0.seconds, addActorsInterval, self, AddMoreActors)
  }

  override def receive: Receive = {
    case ActorCountingEntity.Ready(batchCount, startTime) =>
      // not really goal of this benchmark though:
      // if (log.isDebugEnabled)
      //   log.debug("Actor {} took [{} ns] to initialize and reply-back", sender().path.name, startTime - System.nanoTime())

      pendingAliveConfirmation -= 1
      totalAliveConfirmedActors += 1
      if (totalAliveConfirmedActors % addActorsBatch == 0) {
        val timeToStartBatch = (System.nanoTime() - timesWhenWeStartedBatch(batchCount)).nanos.toMillis
        log.info("FINISHED INITIALIZING ANOTHER {} ACTORS (TOTAL: {}) IN [{} ms]!!!", addActorsBatch, totalAliveConfirmedActors, timeToStartBatch)
      }

    case AddMoreActors =>
      batchCount += 1
      log.info(
        s"#$batchCount Adding:+${addActorsBatch} actors. " +
          s"Total actors before:[${totalStartingActorsInSharding}]. " +
          s"STILL pending alive-confirmation:[${pendingAliveConfirmation}] (so ${pendingAliveConfirmation + addActorsBatch} now). " +
          s"At cluster size:${cluster.state.members.size} nodes)")

      timesWhenWeStartedBatch = timesWhenWeStartedBatch.updated(batchCount, System.nanoTime())

      var i = 0
      while (i < addActorsBatch) {
        totalStartingActorsInSharding += 1
        sharding ! ActorCountingEntity.Start(batchCount, totalStartingActorsInSharding, System.nanoTime())
        i += 1
      }
      pendingAliveConfirmation += addActorsBatch
  }
}

final case object AddMoreActors
