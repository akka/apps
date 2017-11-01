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
  final case class Stored(command: Command) extends Event {
    def render = s"${Logging.simpleName(getClass)}::command: ${command}"
  }

  final case class AppliedSelf(command: Event, timestampApply: Long) extends Event {
    def render = s"${Logging.simpleName(getClass)}::command: ${command}"
  }
  final case class AppliedReplicated(command: Event, timestampApply: Long, concurrent: Boolean, originDc: String, ctxSequenceNr: Long, ctxTimestamp: Long) extends Event {
    def render = s"${Logging.simpleName(getClass)}::command: ${command}"
  }

  final case class AllState(events: List[Event])

  def props(system: ActorSystem, settings: PersistenceMultiDcSettings): Props =
    ReplicatedEntity.props("", Some("introspector"), () => new ReplicatedIntrospector(system), settings)

  def shardingProps(system: ActorSystem, settings: PersistenceMultiDcSettings): Props =
    ReplicatedEntity.props(ShardingTypeName, None, () => new ReplicatedIntrospector(system), settings)

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

  lazy val log = Logging(system, entityId)

  override def initialState: Vector[Event] = Vector.empty

  override def recoveryCompleted(state: Vector[Event], ctx: ActorContext) = {
    log.info("Recovery complete: " + state)

    Vector.empty
  }

  override def detectConcurrentUpdates: Boolean = true

  override def commandHandler = CommandHandler {
    case (i: Inspect, state, ctx) ⇒
      log.info("Inspect arrived; all state: " + AllState(state.toList))
      val replyTo = ctx.sender()
      replyTo ! AllState(state.toList)
      Effect.done

    case (a @ Append(_, payload), state, ctx) ⇒
      log.info(s"$a arrived;")
      val replyTo = ctx.sender()
      Effect.persist(Stored(a)).andThen(x ⇒ {
        log.info("     sending: " + AllState(x.toList))
        replyTo ! AllState(x.toList)
      })
  }



  override def applySelfEvent(event: Event, state: Vector[Event], ctx: SelfEventContext): Vector[Event] = {
    // TODO would be nice if ctx had a logger
    log.info("Applying self event: " + event)
    val s = state :+ AppliedSelf(event, currentTimeMillis())
    log.info("     State so far: : " + s)
    s
  }

  override def applyReplicatedEvent(event: Event, state: Vector[Event], ctx: ReplicatedEventContext): Vector[Event] = {
    // TODO would be nice if ctx had a logger
    log.info("Applying replicated event: " + event)
    val s = state :+ AppliedReplicated(event, currentTimeMillis(), ctx.concurrent, ctx.originDc, ctx.sequenceNr, ctx.timestamp)
    log.info("     State so far: : " + s)
    s
  }

  override def applyEvent(event: Event, state: Vector[Event]): Vector[Event] = ???
}
