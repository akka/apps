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

package com.lightbend.multidc

import akka.persistence.multidc.scaladsl._
import akka.actor.Props
import akka.persistence.multidc.PersistenceMultiDcSettings
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.StartEntity

object ReplicatedCounter {

  sealed trait Command
  case object Get extends Command
  case class Increment(note: String) extends Command

  sealed trait Event
  final case class Incremented(delta: Int, note: String) extends Event

  case object IncrementAck

  def props(settings: PersistenceMultiDcSettings): Props =
    ReplicatedEntity.props("", Some("counter"), () => new ReplicatedCounter, settings)

  def shardingProps(settings: PersistenceMultiDcSettings): Props =
    ReplicatedEntity.props(ShardingTypeName, None, () => new ReplicatedCounter, settings)

  val ShardingTypeName = "counter"

  final case class ShardingEnvelope(entityId: String, cmd: Command)

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case ShardingEnvelope(entityId, cmd) => (entityId, cmd)
  }

  val MaxShards = 100
  def shardId(entityId: String): String = (math.abs(entityId.hashCode) % MaxShards).toString
  val extractShardId: ShardRegion.ExtractShardId = {
    case ShardingEnvelope(entityId, _) => shardId(entityId)
    case StartEntity(entityId) => shardId(entityId)
  }
}

class ReplicatedCounter extends ReplicatedEntity[ReplicatedCounter.Command, ReplicatedCounter.Event, Counter] {
  import ReplicatedCounter._

  override def initialState: Counter = Counter.empty

  override def applyEvent(event: Event, state: Counter): Counter = event match {
    case Incremented(delta, _) => state.applyEvent(Counter.Updated(delta))
  }

  override def commandHandler: CommandHandler = {
    CommandHandler {
      case (Increment(note), state, ctx) =>
        Effect.persist(Incremented(1, note)).andThen { _ =>
          ctx.sender() ! IncrementAck
        }
      case (Get, state, ctx) =>
        ctx.sender() ! state.value.intValue
        Effect.done
    }
  }
}

