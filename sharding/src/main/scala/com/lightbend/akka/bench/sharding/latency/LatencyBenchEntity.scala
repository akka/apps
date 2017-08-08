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

package com.lightbend.akka.bench.sharding.latency

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import akka.persistence.{ DeleteMessagesFailure, DeleteMessagesSuccess, PersistentActor, RecoveryCompleted }
import com.lightbend.akka.bench.sharding.BenchSettings

import scala.concurrent.duration._

object LatencyBenchEntity {

  // commands
  sealed trait EntityCommand { def id: String }
  sealed trait PingLike { def id: String; def pingCreatedNanoTime: Long }
  final case class PingFirst(id: String, pingCreatedNanoTime: Long) extends EntityCommand with PingLike
  final case class PongFirst(original: PingLike, wakeupTimeMicros: Long)
  
  final case class PingSecond(id: String, pingCreatedNanoTime: Long) extends EntityCommand with PingLike
  final case class PongSecond(original: PingLike)

  final case class PersistAndPing(id: String, pingCreatedNanoTime: Long) extends EntityCommand with PingLike
  final case class PersistPingSecond(id: String, pingCreatedNanoTime: Long) extends EntityCommand with PingLike
  final case class RecoveredWithin(ms: Long, events: Long)
  
  // events
  case class PingObserved(sentTimestamp: Long)

  def props() = Props[LatencyBenchEntity]


  // sharding config
  val typeName = "bench-entity"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg: LatencyBenchEntity.EntityCommand => (msg.id, msg)
  }

  def extractShardId(numberOfEntities: Int): ShardRegion.ExtractShardId = {
    case msg: LatencyBenchEntity.EntityCommand => (Math.abs(msg.id.hashCode) % numberOfEntities).toString
  }

  def startRegion(system: ActorSystem) =
    ClusterSharding(system).start(
      typeName,
      LatencyBenchEntity.props(),
      ClusterShardingSettings(system),
      extractEntityId,
      extractShardId(BenchSettings(system).NumberOfShards)
    )

  def proxy(system: ActorSystem) =
    ClusterSharding(system)
      .startProxy(
        typeName,
        None, // don't require the shard role Some("shard"),
        extractEntityId,
        extractShardId(BenchSettings(system).NumberOfShards)
      )
}

class LatencyBenchEntity extends PersistentActor with ActorLogging {
  import LatencyBenchEntity._

  log.info(s"Started ${self.path.name}")
  
  var persistentPingCounter = 0
  def persistenceId: String = self.path.name

  val started = System.nanoTime()
  lazy val recovered = System.nanoTime()

  // def receive = {
  def receiveCommand = {
    case msg: PingFirst =>
      // simple roundtrip
      val recoveryTime = (recovered - started).nanos
      sender() ! PongFirst(msg, wakeupTimeMicros = recoveryTime.toMicros)
    
    case msg: PingSecond =>
      // simple roundtrip
      sender() ! PongSecond(msg)

    case msg: PersistAndPing =>
      // roundtrip with write
      val before = System.nanoTime()
      persist(PingObserved(msg.pingCreatedNanoTime)) { _ =>
        val singlePersistTime = (System.nanoTime() - before).nanos
        PersistenceHistograms.recordSinglePersistTiming(singlePersistTime)
        persistentPingCounter += 1
      }
      persistAll(List.fill(99)(PingObserved(msg.pingCreatedNanoTime))) { _ =>
        persistentPingCounter += 99
        sender() ! PongFirst(msg, started)
        context.stop(self)
      }
      
    case msg: PersistPingSecond =>
      sender() ! RecoveredWithin((recovered - started).nanos.toMillis, persistentPingCounter)
      deleteMessages(lastSequenceNr)
    
    case DeleteMessagesSuccess(s) =>
      log.info(s"Cleared messages (until ${s})")
      context stop self
    case DeleteMessagesFailure(ex, s) =>
      log.warning(s"Failed to clear messages (until ${s}), ex = ${ex}")
      context stop self
      
    case other =>
      log.info("received something else: " + other)
      throw new Exception("What is: " + other.getClass)
  }
  
  def receiveRecover: Receive = {
    case _: PingObserved =>
      persistentPingCounter += 1

    case _: RecoveryCompleted =>
      val recoveryTime = (recovered - started).nanos
      PersistenceHistograms.recordRecoveryPersistTiming(recoveryTime)

  }
  


}
