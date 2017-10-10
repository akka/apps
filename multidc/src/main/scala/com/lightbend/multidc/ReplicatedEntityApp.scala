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

package com.lightbend.multidc

import java.io.File

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.http.management.ClusterHttpManagement
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.persistence.multidc.PersistenceMultiDcSettings
import com.lightbend.multidc.ReplicatedCounter.{Increment, ShardingEnvelope}
import com.typesafe.config.ConfigFactory

import scala.io.StdIn

object ReplicatedEntityApp extends App {
  val rootConfFile = new File("/home/akka/multidc/application.conf")
  val rootConf =
    if (rootConfFile.exists) ConfigFactory.parseFile(rootConfFile)
    else ConfigFactory.empty("no-root-application-conf-found")
  val conf = rootConf.withFallback(ConfigFactory.load())
  println(s"Cloud configuration: ${rootConfFile.exists}")

  implicit val system: ActorSystem = ActorSystem("MultiDcSystem", conf)


  val cluster = Cluster(system)
  ClusterHttpManagement(cluster).start()

  ClusterSharding(system).start(
    typeName = ReplicatedCounter.ShardingTypeName,
    entityProps = ReplicatedCounter.shardingProps(PersistenceMultiDcSettings(system)),
    settings = ClusterShardingSettings(system),
    extractEntityId = ReplicatedCounter.extractEntityId,
    extractShardId = ReplicatedCounter.extractShardId)

  val counterProxy: ActorRef = ClusterSharding(system).startProxy(
    typeName = ReplicatedCounter.ShardingTypeName,
    role = None,
    dataCenter = Some("eu-west"),
    extractEntityId = ReplicatedCounter.extractEntityId,
    extractShardId = ReplicatedCounter.extractShardId)

  HttpApi.startServer("localhost", 8080, counterProxy)

  if (Cluster(system).selfRoles("load-generator")) {
    Thread.sleep(2000)
    val nrEntities = 1000
    val nrIncrements = 1000000
    println(s"Sending load")
    (1 to nrEntities).foreach { i =>
      val entityId = i.toString
      (1 to nrIncrements).foreach { _ =>
        counterProxy ! ShardingEnvelope(entityId, Increment("up you go"))
        Thread.sleep(50)
      }
    }
  }

  StdIn.readLine()
  system.terminate()
}

