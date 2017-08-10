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

import java.util.Locale

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import akka.persistence._
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
  final case class RecoveredWithin(micros: Long, events: Long)

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
      extractShardId(BenchSettings(system).NumberOfShards))

}

class LatencyBenchEntity extends PersistentActor with ActorLogging {
  import LatencyBenchEntity._

  def persistenceId: String = self.path.name

  val started = System.nanoTime()
  lazy val recovered = System.nanoTime()
  lazy val recoveryTime = (recovered - started).nanos

  override def recovery: Recovery = Recovery(toSequenceNr = 100)

  def receiveCommand = {
    case msg: PingFirst =>
      // simple roundtrip
      log.info(s"Started ${self.path.name}, received 1st msg, within ${PrettyDuration.format(recoveryTime)} from start")
      PersistenceHistograms.recordRecoveryPersistTiming(recoveryTime)
      sender() ! PongFirst(msg, wakeupTimeMicros = recoveryTime.toMicros)

    case msg: PingSecond =>
      // simple roundtrip
      sender() ! PongSecond(msg)

    case msg: PersistAndPing =>
      if (lastSequenceNr < 100) {
        val stillNeedToPersistNTimes = 100 - lastSequenceNr

        // roundtrip with write
        val before = System.nanoTime()
        persist(PingObserved(msg.pingCreatedNanoTime)) { _ =>
          val singlePersistTime = (System.nanoTime() - before).nanos
          PersistenceHistograms.recordSinglePersistTiming(singlePersistTime)
          log.info(s"Single persist in ${self.path.name} took ${PrettyDuration.format(singlePersistTime)}")
        }

        if (stillNeedToPersistNTimes > 1) {
          val n: Int = stillNeedToPersistNTimes.toInt - 1
          persistAll(List.fill(n)(PingObserved(msg.pingCreatedNanoTime))) { _ =>
            log.info(s"DONE PERSISTING: at sequence ${lastSequenceNr}")

            sender() ! PongFirst(msg, started)
            context.stop(self)
          }
        }
      } else {
        sender() ! PongFirst(msg, started)
        context.stop(self)
      }

      sender() ! RecoveredWithin((recovered - started).nanos.toMicros, 100)

    case other =>
      log.info("received something else: " + other)
      throw new Exception("What is: " + other.getClass)
  }

  def receiveRecover: Receive = {
    case _: PingObserved =>
    // log.info(s"REPLAY: Ping observed @ ${lastSequenceNr}")

    case _: RecoveryCompleted =>
      PersistenceHistograms.recordRecoveryPersistTiming(recoveryTime)

  }

}

object PrettyDuration {

  /**
   * JAVA API
   * Selects most apropriate TimeUnit for given duration and formats it accordingly, with 4 digits precision
   */
  def format(duration: Duration): String = duration.pretty

  /**
   * JAVA API
   * Selects most apropriate TimeUnit for given duration and formats it accordingly
   */
  def format(duration: Duration, includeNanos: Boolean, precision: Int): String = duration.pretty(includeNanos, precision)

  implicit class PrettyPrintableDuration(val duration: Duration) extends AnyVal {

    /** Selects most apropriate TimeUnit for given duration and formats it accordingly, with 4 digits precision **/
    def pretty: String = pretty(includeNanos = false)

    /** Selects most apropriate TimeUnit for given duration and formats it accordingly */
    def pretty(includeNanos: Boolean, precision: Int = 4): String = {
      require(precision > 0, "precision must be > 0")

      duration match {
        case d: FiniteDuration ⇒
          val nanos = d.toNanos
          val unit = chooseUnit(nanos)
          val value = nanos.toDouble / NANOSECONDS.convert(1, unit)

          s"%.${precision}g %s%s".formatLocal(Locale.ROOT, value, abbreviate(unit), if (includeNanos) s" ($nanos ns)" else "")

        case Duration.MinusInf ⇒ s"-∞ (minus infinity)"
        case Duration.Inf      ⇒ s"∞ (infinity)"
        case _                 ⇒ "undefined"
      }
    }

    def chooseUnit(nanos: Long): TimeUnit = {
      val d = nanos.nanos

      if (d.toDays > 0) DAYS
      else if (d.toHours > 0) HOURS
      else if (d.toMinutes > 0) MINUTES
      else if (d.toSeconds > 0) SECONDS
      else if (d.toMillis > 0) MILLISECONDS
      else if (d.toMicros > 0) MICROSECONDS
      else NANOSECONDS
    }

    def abbreviate(unit: TimeUnit): String = unit match {
      case NANOSECONDS  ⇒ "ns"
      case MICROSECONDS ⇒ "μs"
      case MILLISECONDS ⇒ "ms"
      case SECONDS      ⇒ "s"
      case MINUTES      ⇒ "min"
      case HOURS        ⇒ "h"
      case DAYS         ⇒ "d"
    }
  }

}
