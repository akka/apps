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

import java.util.Date

import akka.persistence.multidc.scaladsl.ReplicatedEntity
import ReplicatedIntrospector._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.StartEntity
import akka.event.Logging
import akka.persistence.multidc.{PersistenceMultiDcSettings, ReplicatedEventContext, SelfEventContext, SpeculativeReplicatedEvent}

object ReplicatedIntrospector {
  sealed trait Command { def id: String }
  case class Inspect(id: String) extends Command
  case class Append(id: String, payload: String) extends Command

  sealed trait Event { def render: String }
  final case class Stored(command: Command, dc: String) extends Event {
    def render = s"Stored::command: ${command}"
  }

  final case class AppliedSelf(command: Event, timestampApply: Long, dc: String) extends Event {
    def render = s"AppliedSelf::command: ${command}" +
      s"apply @  ${new Date(timestampApply)}" +
      s"selfDc @  ${dc}"
  }
  final case class AppliedReplicated(command: Event, timestampApply: Long, selfDc: String, concurrent: Boolean, originDc: String, ctxSequenceNr: Long, ctxTimestamp: Long) extends Event {
    def render = s"AppliedReplicated::command: ${command}  " +
      s"apply @ ${new Date(timestampApply)} " +
      s"concurrent:${concurrent} " +
      s"selfDc:${selfDc} " +
      s"originDc:${originDc} " +
      s"ctxTimestamp:${new Date(ctxTimestamp)} " +
      s"ctxSequenceNr:${ctxSequenceNr} "
  }

  final case class AllState(events: List[Event])

  def props(system: ActorSystem, settings: PersistenceMultiDcSettings): Props =
    ReplicatedEntity.props("", "introspector", () => new ReplicatedIntrospector(system), settings)

  def shardingProps(system: ActorSystem, settings: PersistenceMultiDcSettings): Props =
    ReplicatedEntity.clusterShardingProps(ShardingTypeName, () => new ReplicatedIntrospector(system), settings)

  val ShardingTypeName = "introspector"

  final case class ShardingEnvelope(entityId: String, cmd: Command)

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case c: Command                      ⇒ (c.id, c)
    case ShardingEnvelope(entityId, cmd) ⇒ (entityId, cmd)
    case evt: SpeculativeReplicatedEvent ⇒ (evt.entityId, evt)
  }

  val MaxShards = 100
  def shardId(entityId: String): String = (math.abs(entityId.hashCode) % MaxShards).toString
  val extractShardId: ShardRegion.ExtractShardId = {
    case c: Command                      ⇒ shardId(c.id)
    case ShardingEnvelope(entityId, _)   ⇒ shardId(entityId)
    case evt: SpeculativeReplicatedEvent ⇒ shardId(evt.entityId)
    case StartEntity(entityId)           ⇒ shardId(entityId)
  }
}

class ReplicatedIntrospector(system: ActorSystem) extends ReplicatedEntity[Command, Event, Vector[Event]] {

  override def initialState: Vector[Event] = Vector.empty

  override def recoveryCompleted(ctx: ActorContext, state: Vector[Event]) = {
    log.info("Recovery complete: " + state)
    Effect.none
  }

  override def commandHandler = CommandHandler {
    case (ctx, state, i: Inspect) ⇒
      log.info("Inspect arrived; all state: " + AllState(state.toList))
      val replyTo = ctx.sender()
      replyTo ! AllState(state.toList)
      Effect.none

    case (ctx, state, a @ Append(_, payload)) ⇒
      log.info(s"$a arrived;")
      val replyTo = ctx.sender()
      Effect.persist(Stored(a, selfDc)).andThen(x ⇒ {
        log.info("     sending: " + AllState(x.toList))
        replyTo ! AllState(x.toList)
      })
  }



  override def selfEventHandler(ctx: SelfEventContext, state: Vector[Event], event: Event): Vector[Event] = {
    log.info("Applying self event: " + event)
    val s = state :+ AppliedSelf(event, currentTimeMillis(), selfDc)
    log.info("     State so far: : " + s)
    s
  }

  override def replicatedEventHandler(ctx: ReplicatedEventContext, state: Vector[Event], event: Event): Vector[Event] = {
    log.info("Applying replicated event: " + event)
    val s = state :+ AppliedReplicated(event, currentTimeMillis(), selfDc, ctx.concurrent, ctx.originDc, ctx.sequenceNr, ctx.timestamp)
    log.info("     State so far: : " + s)
    s
  }

  override def eventHandler(state: Vector[Event], event: Event): Vector[Event] = ???
}
