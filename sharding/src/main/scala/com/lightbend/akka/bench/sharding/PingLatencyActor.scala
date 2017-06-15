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

package com.lightbend.akka.bench.sharding

import akka.actor.{ Actor, ActorLogging, ActorPath, CoordinatedShutdown, Props, ReceiveTimeout, RootActorPath, Terminated }
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.routing.{ ClusterRouterGroup, ClusterRouterGroupSettings }
import akka.cluster.sharding.ShardRegion.GracefulShutdown
import akka.cluster.{ Cluster, ClusterEvent }
import akka.routing.{ BroadcastGroup, Router }
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.{ ActorMaterializer, KillSwitches, ThrottleMode }
import com.lightbend.akka.bench.sharding.ShardingLatencyApp.system
import org.HdrHistogram.Histogram

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
      // don't start benching until all nodes up
      if (seenUpNodes == BenchSettings(system).TotalNodes) {
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
  val proxy = BenchEntity.proxy(context.system)

  val pongRecipient = context.watch(context.actorOf(
    Props(new PongRecipient),
    "pong-recipient"))

  implicit val materializer = ActorMaterializer()(context.system)
  val killSwitch =
    Source(0L to settings.NumberOfPings)
      // just chill a bit to give sharding time to start, doesn't really belong here but whatever
      .initialDelay(10.second)
      .throttle(settings.PingsPerSecond / 20, 50.millis, settings.PingsPerSecond, ThrottleMode.shaping)
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(Sink.foreach { n =>
        val entityId = (n % settings.UniqueEntities).toString
        val msg = BenchEntity.PersistAndPing(entityId, System.nanoTime())
        proxy.tell(msg, pongRecipient)
      })(Keep.left).run()

  def receive = {

    case Terminated(_) =>
      killSwitch.shutdown()
      context.stop(self)

  }


}

class PongRecipient extends Actor {
  case object Tick
  val maxRecordedTimespan = 30 * 1000
  val pingPersistPongMsHistogram = new Histogram(maxRecordedTimespan, 3)

  context.setReceiveTimeout(20.seconds)

  def receive = {

    case BenchEntity.Pong(ping: BenchEntity.PersistAndPing) =>
      val msSpan = (System.nanoTime() - ping.sentTimestamp) / 1000000
      pingPersistPongMsHistogram.recordValue(msSpan)

    case ReceiveTimeout =>
      printHistograms()
      context.stop(self)

  }

  def printHistograms(): Unit = {
    println("Histogram of persisted ping-pongs")
    pingPersistPongMsHistogram.outputPercentileDistribution(System.out, 1.0)
  }

}
