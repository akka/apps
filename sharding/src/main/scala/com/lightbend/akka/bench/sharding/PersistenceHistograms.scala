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

import akka.actor.{Actor, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberLeft
import com.lightbend.akka.bench.sharding.PersistenceHistograms.PrintHistograms
import org.HdrHistogram.Histogram

/**
  * Thread safe histograms updated from the persistent entity, an actor (should be a single one per actor system)
  * periodically printing the histograms to stdout
  */
object PersistenceHistograms {

  val persistTiming  = new Histogram(20 * 1000, 3)
  val recoveryTiming = new Histogram(60 * 1000, 3)

  object PrintHistograms

  def props() = Props[PersistenceHistograms]
}

class PersistenceHistograms extends Actor {

  Cluster(context.system).subscribe(self, classOf[MemberLeft])

  override def receive: Receive = {
    case _: MemberLeft =>
      // as soon as members start leaving we can shutdown as well

      // not really useful yet
      // println("Histogram of persisted entity recoveries")
      // PersistenceHistograms.recoveryTiming.outputPercentileDistribution(System.out, 1.0)

      println("Histogram of persists")
      PersistenceHistograms.persistTiming.outputPercentileDistribution(System.out, 1.0)

      context.system.terminate()

  }
}
