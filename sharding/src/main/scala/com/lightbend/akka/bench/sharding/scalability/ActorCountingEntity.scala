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

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import com.lightbend.akka.bench.sharding.BenchSettings

object ActorCountingEntity {

  sealed trait HasId { def id: Long }
  // sent from master to entity
  final case class Start(override val id: Long, sentTimestamp: Long) extends HasId
  // entity replies with that once it has started, the time is the sender's time, 
  // so the sender can calculate how long it took for the actor to get the message
  final case class Ready(sentTimestamp: Long)

  def props() = Props[ActorCountingEntity]

  // sharding config
  val typeName = "bench-entity"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg: ActorCountingEntity.HasId => (msg.id.toString, msg)
  }

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case msg: ActorCountingEntity.HasId => (Math.abs(msg.id.hashCode) % numberOfShards).toString
  }

  def startRegion(system: ActorSystem) =
    ClusterSharding(system).start(
      typeName,
      ActorCountingEntity.props(),
      ClusterShardingSettings(system).withRole("shard"),
      extractEntityId,
      extractShardId(BenchSettings(system).NumberOfShards)
    )

  def proxy(system: ActorSystem): ActorRef =
    ClusterSharding(system)
      .startProxy(
        typeName,
        Some("shard"),
        extractEntityId,
        extractShardId(BenchSettings(system).NumberOfShards)
      )
}


/** Many many many instances of this Actor will be started in sharding. */
class ActorCountingEntity extends Actor {

  override def receive: Receive = {
    case ActorCountingEntity.Start(_, senderTimestamp) =>
      sender() ! ActorCountingEntity.Ready(senderTimestamp)
      // stay around in memory, forevermore!
  }
}
