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

package com.lightbend.akka.bench.ddata

import java.io.{ByteArrayOutputStream, PrintStream}

import akka.Done
import akka.actor.Status.Success
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Terminated}
import akka.cluster.ddata.{DistributedData, ORSet, ORSetKey, Replicator}
import akka.cluster.{Cluster, MemberStatus}
import akka.cluster.ddata.Replicator.WriteConsistency
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.lightbend.akka.bench.ddata.DDataHost.{Add, Added}
import org.HdrHistogram.Histogram

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object DDataBenchmarkCoordinator {


  def singletonProxyProps(system: ActorSystem) =  ClusterSingletonProxy.props(
    singletonManagerPath = "/user/" + DistributedDataBenchmark.CoordinatorManager,
    settings = ClusterSingletonProxySettings(system).withRole("master"))

  case class TriggerTest(nodeCount: Int,
                         rounds: Int,
                         consistency: WriteConsistency,
                         outputActor: ActorRef)
  case object Start
  case object TestAck

  // sends these and then finally a akka.actor.Success(Done)
  case class PartialOutput(text: String)



  case class TestRun(
                      outActor: ActorRef,
                      totalNodes: Int,
                      nodesLeft: Int,
                      roundsLeft: Int,
                      started: Long,
                      writeConsistency: WriteConsistency,
                      disseminationTiming: Histogram = new Histogram(20 * 1000 * 1000, 3)) {

    def oneNodeLessLeft() = copy(nodesLeft = this.nodesLeft - 1)

    def nanosSinceStart = (System.nanoTime() - started).nanos

    def nodesDone = totalNodes - nodesLeft

    def startNextRound = copy(
      roundsLeft = this.roundsLeft - 1,
      started = System.nanoTime(),
      nodesLeft = totalNodes // we're not always 2.12 so no trailing comma
    )

    def recordTiming(timing: Long) = disseminationTiming.recordValue(timing)

  }

}

class DDataBenchmarkCoordinator() extends Actor with ActorLogging {
  import DDataBenchmarkCoordinator._

  val cluster = Cluster(context.system)
  import context.dispatcher

  val coordinator = context.actorOf(
    DDataBenchmarkCoordinator.singletonProxyProps(context.system),
    name = "coordinatorProxy")

  def receive = idle

  def idle: Receive = {
    case TriggerTest(nodes, rounds, writeConsistency, outActor) =>
      val upNodes = cluster.state.members.count(_.status == MemberStatus.Up)

      if (upNodes < nodes) {
        outActor ! PartialOutput(s"Requested to run on $nodes but cannot because only have $upNodes")
        outActor ! Success(Done)
      } else {
        outActor ! PartialOutput("Starting benchmark run")
        ddataAdd("stuff", writeConsistency)
        context.watch(outActor)
        context.become(testing(TestRun(outActor, nodes, nodes, rounds, System.nanoTime(), writeConsistency)))

      }

    case Terminated(_) => // some old out actor died, we don't care
  }

  def testing(state: TestRun): Actor.Receive = {
    case TriggerTest(_, _, _, newOut) =>
      newOut ! PartialOutput("Error: Already running test")
      newOut ! Success(Done)

    case Start =>
      ddataAdd(Random.nextString(8), state.writeConsistency)

    case Added =>
      if (state.nodesLeft > 1)
        context.become(testing(state.oneNodeLessLeft()))
      else {
        val timeTaken = state.nanosSinceStart
        state.outActor ! PartialOutput(s"DData disseminated to ${state.totalNodes} nodes in ${timeTaken.toMillis} ms.")
        state.recordTiming(timeTaken.toMicros)

        if (state.roundsLeft > 1) {
          context.become(testing(state.startNextRound))
          self ! Start
        }
        else {
          outputResult(state)
          context.unwatch(state.outActor)
          state.outActor ! Success(Done)
          context.become(idle)
        }
      }

    case Terminated(state.outActor) =>
      log.info("Output actor died, cancelling test run")
      context.become(idle)


    case x =>
      log.info("Unexpected msg {}", x)
      state.outActor ! Success(Done)
      context.become(idle)
  }

  def ddataAdd(s: String, writeConsistency: WriteConsistency) =
    context.actorSelection("/user/" + DDataHost.Name) ! DDataHost.Add(s, writeConsistency)


  def outputResult(state: TestRun)(implicit ec: ExecutionContext): Unit = {
    def percentile(p: Double): Double = state.disseminationTiming.getValueAtPercentile(p)

    state.outActor ! PartialOutput("=== Distributed Data Benchmark " +
      f"50%%ile: ${percentile(50.0)}%.0f µs, " +
      f"90%%ile: ${percentile(90.0)}%.0f µs, " +
      f"99%%ile: ${percentile(99.0)}%.0f µs\n")

    state.outActor ! PartialOutput("Histogram of dissemination latencies in microseconds.")

    val percentileDist: String = {
      val bos = new ByteArrayOutputStream()
      val ps = new PrintStream(bos)
      state.disseminationTiming.outputPercentileDistribution(ps, 1.0)
      bos.toString("UTF-8")
    }

    state.outActor ! PartialOutput(percentileDist)

  }

}
