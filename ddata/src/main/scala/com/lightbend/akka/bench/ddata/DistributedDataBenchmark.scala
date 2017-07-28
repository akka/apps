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

import akka.actor.{ ActorSystem, PoisonPill, Props }
import akka.cluster.Cluster
import java.io.File

import akka.cluster.singleton.{ ClusterSingletonManager, ClusterSingletonManagerSettings }
import com.typesafe.config.ConfigFactory

import scala.util.Try

object DistributedDataBenchmark extends App {

  final val CoordinatorManager = "coordinatorManager"

  // setup for cloud env -------------------------------------------------------------
  val isMaster = Option(System.getenv("MASTER")).isDefined
  val rootConfFile = new File("/home/akka/root-application.conf")
  val rootConf = 
    if (rootConfFile.exists) ConfigFactory.parseFile(rootConfFile) 
    else ConfigFactory.empty("no-root-application-conf-found")

  val masterRole =
    if (isMaster) ConfigFactory.parseString("akka.cluster.roles=[master]")
    else ConfigFactory.empty()

  val conf = masterRole.withFallback(rootConf.withFallback(ConfigFactory.load()))
  // end of setup for cloud env ------------------------------------------------------


  implicit val system = ActorSystem("DistributedDataSystem", conf)
  

  val cluster = Cluster(system)

  cluster.registerOnMemberUp {
    system.actorOf(Props[DDataHost], DDataHost.Name)

    // only the "master" node runs the singleton
    if (cluster.selfRoles("master")) {
      system.actorOf(
        ClusterSingletonManager.props(
          singletonProps = Props[DDataBenchmarkCoordinator],
          terminationMessage = PoisonPill,
          settings = ClusterSingletonManagerSettings(system).withRole("master")),
        name = CoordinatorManager)


      val httpHost = conf.getString("bench.ddata.http-api.host")
      val httpPort = conf.getInt("bench.ddata.http-api.port")

      HttpApi.startServer(httpHost, httpPort)

    }
  }

  scala.io.StdIn.readLine() // TODO not sure if readline will work well with starting it via scripts...
  system.terminate()
}
