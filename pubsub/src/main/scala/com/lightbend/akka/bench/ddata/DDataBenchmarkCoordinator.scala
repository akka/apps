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

import akka.actor.{Actor, ActorLogging}
import org.HdrHistogram.Histogram

import scala.util.Random
import scala.concurrent.duration._

object DDataBenchmarkCoordinator {
  case object Start
}

class DDataBenchmarkCoordinator() extends Actor with ActorLogging {
  import DDataBenchmarkCoordinator._

  final val NumRounds = context.system.settings.config.getInt("bench.ddata.num-rounds")

  val disseminationTiming = new Histogram(20 * 1000 * 1000, 3)
  val NumNodes = context.system.settings.config.getInt("akka.cluster.min-nr-of-members")

  context.actorSelection("/user/" + DDataHost.Name) ! DDataHost.Add("stuff")

  def receive = testing(NumNodes, NumRounds, System.nanoTime())

  def testing(nodesLeft: Int, roundsLeft: Int, started: Long): Actor.Receive = {
    case Start =>
      ddataAdd(Random.nextString(8))
    case DDataHost.Added =>
      if (nodesLeft > 1)
        context.become(testing(nodesLeft - 1, roundsLeft, started))
      else {
        val timeTaken = (System.nanoTime() - started).nanos
        log.info(s"DData disseminated to {} nodes in {} ms.", NumNodes, timeTaken.toMillis)
        disseminationTiming.recordValue(timeTaken.toMicros)

        if (roundsLeft > 1) {
          context.become(testing(NumNodes, roundsLeft - 1, System.nanoTime()))
          self ! Start
        }
        else {
          printStats()
        }
      }
  }

  def ddataAdd(s: String) =
    context.actorSelection("/user/" + DDataHost.Name) ! DDataHost.Add(s)

  def printStats() = {
    def percentile(p: Double): Double = disseminationTiming.getValueAtPercentile(p)

    println(s"=== Distributed Data Benchmark " +
      f"50%%ile: ${percentile(50.0)}%.0f µs, " +
      f"90%%ile: ${percentile(90.0)}%.0f µs, " +
      f"99%%ile: ${percentile(99.0)}%.0f µs")

    println("Histogram of dissemination latencies in microseconds.")
    disseminationTiming.outputPercentileDistribution(System.out, 1.0)
  }

}
