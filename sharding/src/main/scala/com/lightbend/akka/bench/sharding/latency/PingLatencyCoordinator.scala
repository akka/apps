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

import java.util.concurrent.TimeoutException

import akka.actor.{ Actor, ActorLogging, ActorRef, CoordinatedShutdown, Props, ReceiveTimeout, Terminated }
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.{ Cluster, ClusterEvent }
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.{ ActorMaterializer, Attributes, KillSwitches, ThrottleMode }
import com.lightbend.akka.bench.sharding.BenchSettings
import org.HdrHistogram.Histogram

import scala.concurrent.Future
import scala.concurrent.duration._


object PingLatencyCoordinator {

  def props() = Props(new PingLatencyCoordinator)
}

class PingLatencyCoordinator extends Actor with ActorLogging {

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
        context.watch(system.actorOf(PingingActor.props(), "pinging-actor"))
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

  def props() = Props(new PingingActor())
}

class PingingActor extends Actor with ActorLogging {

  val settings = BenchSettings(context.system)
  val proxy = LatencyBenchEntity.proxy(context.system)

  val pongRecipient = context.watch(context.actorOf(Props(new PongRecipient), "pong-recipient"))

  implicit val materializer = ActorMaterializer()(context.system)
  
  val persistMode: Boolean =
    sys.env.get("MODE").exists(_ == "persist")
  log.info(Console.RED + s"Using mode: ${persistMode}" + Console.RESET)
  
  val killSwitch =
    Source(0L to Int.MaxValue - 10)
      // just chill a bit to give sharding time to start, doesn't really belong here but whatever
      .throttle(settings.PingsPerSecond, 1.second, settings.PingsPerSecond, ThrottleMode.shaping)
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(Sink.foreach { n =>
        val entityId = (n % settings.UniqueEntities).toString

        val msg =
          if (persistMode) LatencyBenchEntity.PersistAndPing(entityId, System.nanoTime())
          else LatencyBenchEntity.PingFirst(entityId, System.nanoTime())

        proxy.!(msg)(sender = pongRecipient)
      })(Keep.left).run()

  def receive = {

    case Terminated(_) =>
      killSwitch.shutdown()
      context.stop(self)

  }


}

class PongRecipient extends Actor with ActorLogging {
  case object Tick
  import context.dispatcher
  
  var pongsReceived = 0L
  val maxRecordedTimespan = 20 * 1000 * 1000
  
  // time between first message to sharded actor and it coming back:
  val initialPingPongTiming = new Histogram(maxRecordedTimespan, 3)
  // time to ping an already started sharded actor: 
  val secondPingPongTiming = new Histogram(maxRecordedTimespan, 3)
  // time between sending message, and the sharded actor finishing start:
  val wakeupTiming = new Histogram(maxRecordedTimespan, 3)

  context.system.scheduler.schedule(2.second, 2.second, self, Tick)
  context.setReceiveTimeout(20.seconds)

  def receive = {
    case LatencyBenchEntity.PongFirst(ping, wakeupTime) =>
      val msSpan = (System.nanoTime() - ping.sentTimestamp).nanos
      pongsReceived += 1
      initialPingPongTiming.recordValue(msSpan.toMillis)
      wakeupTiming.recordValue(msSpan.toMillis)
      
      sender() ! LatencyBenchEntity.PingSecond(ping.id, System.nanoTime())
    
    case LatencyBenchEntity.PongSecond(ping) =>
      val msSpan = (System.nanoTime() - ping.sentTimestamp).nanos
      pongsReceived += 1
      secondPingPongTiming.recordValue(msSpan.toMillis)
      // keep that sharded actor alive
      
    case Tick =>
      printHistograms()
      
    case ReceiveTimeout =>
      log.info("Terminating, received {} pongs, and receive timeout triggered ", pongsReceived)
      printHistograms()
      context.stop(self)

  }

  def printHistograms(): Unit = {
    println("====== initial ping pong timing (shard wake-up) ======")
    initialPingPongTiming.outputPercentileDistribution(System.out, 1.0)
    println("====== ping pong timing (alive actor) ======")
    secondPingPongTiming.outputPercentileDistribution(System.out, 1.0)
  }
  
}
