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

package apps

import akka.actor.{Actor, ActorLogging}
import org.HdrHistogram.Histogram

import scala.util.Random

object DDataBenchmarkCoordinator {
  case object Start
}

class DDataBenchmarkCoordinator() extends Actor with ActorLogging {
  import DDataBenchmarkCoordinator._

  final val NumRounds = context.system.settings.config.getInt("bench.ddata.num-rounds")

  val disseminationTiming = new Histogram(20 * 1000, 3)
  val NumNodes = context.system.settings.config.getInt("akka.cluster.min-nr-of-members")

  context.actorSelection("/user/" + DDataHost.Name) ! DDataHost.Add("stuff")

  def receive = testing(NumNodes, NumRounds, System.currentTimeMillis())

  def testing(nodesLeft: Int, roundsLeft: Int, started: Long): Actor.Receive = {
    case Start =>
      ddataAdd(Random.nextString(8))
    case DDataHost.Added =>
      if (nodesLeft > 1)
        context.become(testing(nodesLeft - 1, roundsLeft, started))
      else {
        val timeTakenMs = System.currentTimeMillis() - started
        log.info(s"DData disseminated to {} nodes in {} ms.", NumNodes, timeTakenMs)
        disseminationTiming.recordValue(timeTakenMs)

        if (roundsLeft > 1) {
          context.become(testing(NumNodes, roundsLeft - 1, System.currentTimeMillis()))
          self ! Start
        }
        else {
          disseminationTiming.outputPercentileDistribution(System.out, 1.0)
        }
      }
  }

  def ddataAdd(s: String) =
    context.actorSelection("/user/" + DDataHost.Name) ! DDataHost.Add(s)

}
