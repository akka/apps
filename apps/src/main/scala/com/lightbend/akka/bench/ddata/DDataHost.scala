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

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator.{WriteAll, WriteLocal, WriteMajority, WriteTo}
import akka.cluster.ddata.{DistributedData, ORSet, ORSetKey, Replicator}
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}

import scala.concurrent.duration._

object DDataHost {

  final val Name = "ddataHost"

  case class Add(el: String)
  case object Added

}

class DDataHost extends Actor {
  import DDataHost._

  implicit val cluster = Cluster(context.system)

  val writeTimeout = context.system.settings.config.getDuration("bench.ddata.write-timeout", TimeUnit.MICROSECONDS).micros
  val writeConsistency = context.system.settings.config.getString("bench.ddata.write-consistency") match {
    case "local" => WriteLocal
    case "majority" => WriteMajority(writeTimeout)
    case "all" => WriteAll(writeTimeout)
    case numNodes => WriteTo(numNodes.toInt, writeTimeout)
  }

  val coordinator = context.actorOf(
    ClusterSingletonProxy.props(
      singletonManagerPath = "/user/" + DistributedDataBenchmark.CoordinatorManager,
      settings = ClusterSingletonProxySettings(context.system)),
    name = "coordinatorProxy")

  val replicator = DistributedData(context.system).replicator
  val DataKey = ORSetKey[String]("benchmark")

  replicator ! Replicator.Subscribe(DataKey, self)

  def receive = {
    case Add(el) =>
      replicator ! Replicator.Update(DataKey, ORSet.empty[String], writeConsistency)(_ + el)

    case c @ Replicator.Changed(DataKey) =>
      coordinator ! Added
  }

}
