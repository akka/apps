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

import akka.actor.{ Actor, ActorLogging, Props }
import org.HdrHistogram.Histogram

/**
 * Thread safe histograms updated from the persistent entity, an actor (should be a single one per actor system)
 * periodically printing the histograms to stdout
 */
object PersistenceHistograms {

  val persistTiming = new Histogram(20 * 1000, 3)
  val recoveryTiming = new Histogram(60 * 1000, 3)

  object PrintHistograms

  def props() = Props[PersistenceHistograms]
}

class PersistenceHistograms extends Actor with ActorLogging {
  import scala.concurrent.duration._
  import context.dispatcher

  context.system.scheduler.schedule(3.seconds, 3.seconds, self, "ping")
  
  override def receive: Receive = {
    case _ =>
      println("========= Histogram of sharded actor wakeup times ======== ")
      PersistenceHistograms.recoveryTiming.outputPercentileDistribution(System.out, 1.0)
      
//      println("========= Histogram of persist times ======== ")
//      PersistenceHistograms.persistTiming.outputPercentileDistribution(System.out, 1.0)

  }
}
