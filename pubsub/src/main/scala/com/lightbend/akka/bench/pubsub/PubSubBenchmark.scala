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

package com.lightbend.akka.bench.pubsub

import scala.util.Try
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.Cluster
import java.io.File

import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.typesafe.config.ConfigFactory

object PubSubBenchmark extends App {
  final val CoordinatorManager = "pubSubCoordinatorManager"

  // setup for clound env -------------------------------------------------------------
  val rootConfFile = new File("/home/akka/root-application.conf")
  val rootConf =
    if (rootConfFile.exists) ConfigFactory.parseFile(rootConfFile)
    else ConfigFactory.empty("no-root-application-conf-found")

  val conf = rootConf.withFallback(ConfigFactory.load())
  // end of setup for clound env ------------------------------------------------------

  val systemName = Try(conf.getString("akka.system-name")).getOrElse("Benchmark")
  implicit val system = ActorSystem(systemName, conf)

  system.actorOf(PubSubHost.props(), PubSubHost.Name)

  if (Cluster(system).selfRoles("master")) {
    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = BenchmarkCoordinator.props(),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system).withRole("master")),
      name = CoordinatorManager)

    val proxy = system.actorOf(ClusterSingletonProxy.props(
      singletonManagerPath = "/user/" + PubSubBenchmark.CoordinatorManager,
      settings = ClusterSingletonProxySettings(system).withRole("master")),
      name = "coordinatorProxy")

    HttpApi.startServer("localhost", 8080, proxy)

    scala.io.StdIn.readLine()
    system.terminate()
  }
}
