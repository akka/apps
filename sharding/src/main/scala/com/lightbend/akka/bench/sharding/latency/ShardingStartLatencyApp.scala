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

import java.net.InetAddress

import akka.actor.{ ActorSystem, Props }
import akka.cluster.Cluster
import akka.cluster.http.management.ClusterHttpManagement
import com.lightbend.akka.bench.sharding.BenchmarkConfig

import scala.util.Try

object ShardingStartLatencyApp extends App {

  // setup for clound env -------------------------------------------------------------
  val conf = BenchmarkConfig.load()
  // end of setup for clound env ------------------------------------------------------ 

  val systemName = Try(conf.getString("akka.system-name")).getOrElse("ShardingStartLatencySystem")
  implicit val system = ActorSystem(systemName, conf)
  
  // management -----------
  val cluster = Cluster(system)
  ClusterHttpManagement(cluster).start()
  // end of management ----

  val selfHostName = InetAddress.getLocalHost.getHostName
  if (selfHostName contains "akka-sharding-001") { 
    system.actorOf(Props[PingLatencyCoordinator], "bench-coordinator")
    println(Console.RED + s"Started as PingLatencyCoordinator (${selfHostName})" + Console.RESET)
  } else {
    LatencyBenchEntity.startRegion(system)
    system.actorOf(PersistenceHistograms.props(), "persistence-histogram-printer")
    println(Console.GREEN + s"Started as LatencyBenchEntity (${selfHostName})" + Console.RESET)
  }
}
