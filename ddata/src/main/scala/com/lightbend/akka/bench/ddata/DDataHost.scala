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
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata.{DistributedData, ORSet, ORSetKey, Replicator}

object DDataHost {

  final val Name = "ddataHost"

  case class Add(el: String, writeConsistency: WriteConsistency)
  case object Added

}

class DDataHost extends Actor with ActorLogging {
  import DDataHost._

  implicit val cluster = Cluster(context.system)

  val coordinator = context.actorOf(
    DDataBenchmarkCoordinator.singletonProxyProps(context.system),
    name = "coordinatorProxy")

  val replicator = DistributedData(context.system).replicator
  val DataKey = ORSetKey[String]("benchmark")

  replicator ! Replicator.Subscribe(DataKey, self)

  def receive = {
    case Add(el, writeConsistency) =>
      replicator ! Replicator.Update(DataKey, ORSet.empty[String], writeConsistency)(_ + el)

    case c @ Replicator.Changed(DataKey) =>
      // this is the subscribed change, not the ack on the update
      coordinator ! Added

    case c @ Replicator.UpdateTimeout(DataKey, _) =>
      log.warning("Got write timeout")
  }

}
