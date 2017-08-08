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

import akka.actor.{ Actor, ActorLogging, ActorRef, CoordinatedShutdown, Props, ReceiveTimeout, Terminated }
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.{ Cluster, ClusterEvent }
import akka.stream._
import akka.stream.scaladsl.{ Keep, Sink, Source }
import com.lightbend.akka.bench.sharding.BenchSettings
import com.lightbend.akka.bench.sharding.latency.LatencyBenchEntity.PingFirst
import org.HdrHistogram.Histogram

import scala.concurrent.duration._


object PingLatencyCoordinator {

  def props(region: ActorRef): Props = Props(new PingLatencyCoordinator(region))
}

class PingLatencyCoordinator(region: ActorRef) extends Actor with ActorLogging {

  val system = context.system
  val cluster = Cluster(system)

  cluster.subscribe(self, ClusterEvent.InitialStateAsEvents, classOf[ClusterEvent.MemberUp])
  var seenUpNodes = 0

  override def receive: Receive = {
    case m: MemberUp =>
      seenUpNodes += 1
      log.info("Number of UP nodes: [{}]", seenUpNodes)

      // don't start benching until all nodes up
      if (seenUpNodes == BenchSettings(system).MinimumNodes) {
        log.info("Saw [{}] nodes UP starting bench", seenUpNodes)
        context.watch(system.actorOf(PingingActor.props(region), "pinging-actor"))
        context.become(benchmarking)
      }
  }

  def benchmarking: Actor.Receive = {
    case Terminated(_) =>
      log.info("My work here is done")
      CoordinatedShutdown(system).run()
  }

}

/**
 * Sends a configurable number of ping-pongs through sharding, half triggers a persist before pong and half pure
 * in memory to measure sharding vs sharding + persistence
 */
object PingingActor {

  def props(region: ActorRef): Props = Props(new PingingActor(region))
}

class PingingActor(region: ActorRef) extends Actor with ActorLogging {
  val settings = BenchSettings(context.system)

  val pongRecipient = context.watch(context.actorOf(Props(new PongRecipient(region)), "pong-recipient"))

  implicit val materializer = ActorMaterializer()(context.system)

  // will be set once first ping/pong completes and we know the coordinator is alive
  var killSwitch: Option[UniqueKillSwitch] = None

  override def preStart: Unit = {
    region ! PingFirst("wake-up-ping", System.nanoTime())
  }
  
  def receive = {
    case LatencyBenchEntity.PongFirst(ping, wakeup) =>
      log.info(Console.GREEN + s"=== FIRST WAKE UP TOOK: ${wakeup.micros} μs ===" + Console.RESET)
      log.info(s"Starting benchmark, hitting ${settings.UniqueEntities} unique entities in [${settings.NumberOfShards}] shards")

      killSwitch = Some(
        Source(0L to Int.MaxValue - 10)
          // just chill a bit to give sharding time to start, doesn't really belong here but whatever
          .throttle(settings.PingsPerSecond, 1.second, settings.PingsPerSecond, ThrottleMode.shaping)
          .viaMat(KillSwitches.single)(Keep.right)
          .toMat(Sink.foreach { n =>
            val entityId = (n % settings.UniqueEntities).toString

            // PERSIST + PONG:
            // val msg = LatencyBenchEntity.PersistAndPing(entityId, System.nanoTime())
            // just PING/PONG
            val msg = LatencyBenchEntity.PingFirst(entityId, System.nanoTime())

            region.!(msg)(sender = pongRecipient)
          })(Keep.left).run()
      )

    case Terminated(_) =>
      context.stop(self)
  }

  override def postStop(): Unit = {
    killSwitch.foreach(_.shutdown())
  }


}

class PongRecipient(region: ActorRef) extends Actor with ActorLogging {
  case object Tick

  import context.dispatcher

  var pongsReceived = 0L
  val maxRecordedTimespan = 20 * 1000 * 1000

  // time between first message to sharded actor and it coming back:
  val initialWakeUpPingPongTiming = new Histogram(maxRecordedTimespan, 3)
  // time to ping an already started sharded actor: 
  val secondPingPongTiming = new Histogram(maxRecordedTimespan, 3)
  // given 100 persisted messages, how long did a *replay* take
  val replayTiming = new Histogram(maxRecordedTimespan, 3)

  context.system.scheduler.schedule(2.second, 2.second, self, Tick)
  context.setReceiveTimeout(20.seconds)

  def receive = {
    case LatencyBenchEntity.PongFirst(ping, _) =>
      val msSpan = (System.nanoTime() - ping.pingCreatedNanoTime).nanos
      pongsReceived += 1
      initialWakeUpPingPongTiming.recordValue(msSpan.toMicros)

      // we know it has stopped itself now! so this will be a wakeup with replay!
      region ! LatencyBenchEntity.PingSecond(ping.id, System.nanoTime())

    case LatencyBenchEntity.RecoveredWithin(ms, events) =>
      replayTiming.recordValue(ms)

    case LatencyBenchEntity.PongSecond(ping) =>
      val msSpan = (System.nanoTime() - ping.pingCreatedNanoTime).nanos
      pongsReceived += 1
      secondPingPongTiming.recordValue(msSpan.toMicros)
    // keep that sharded actor alive

    case Tick =>
      printHistograms()

    case ReceiveTimeout =>
      log.info("Terminating, received {} pongs, and receive timeout triggered ", pongsReceived)
      printHistograms()
      context.stop(self)

  }

  def printHistograms(): Unit = {
    println("====== wake up and persist 100 events (shard wake-up) ======")
    initialWakeUpPingPongTiming.outputPercentileDistribution(System.out, 1.0)
    //    println("====== ping pong timing (alive actor) ======")
    //    secondPingPongTiming.outputPercentileDistribution(System.out, 1.0)
    println("====== replay time of 100 events, wakeup via sharding (replay) ======")
    replayTiming.outputPercentileDistribution(System.out, 1.0)
  }

}
