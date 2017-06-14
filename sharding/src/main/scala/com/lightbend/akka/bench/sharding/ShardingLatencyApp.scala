/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.typesafe.com>
 */
package com.lightbend.akka.bench.sharding

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object ShardingLatencyApp extends App {

  val port = args(0).toInt
  val role = if (port == 2551) "bench" else "shard"
  println(s"role: $role")


  implicit val system = ActorSystem("cluster",
    ConfigFactory.parseString(
      s"""
        akka.remote.netty.tcp.port = $port
        akka.cluster.roles = [ $role ]
      """).withFallback(ConfigFactory.load()))


  if (role == "bench") {
    system.actorOf(Props[PingLatencyCoordinator], "bench-coordinator")
  } else {
    BenchEntity.startRegion(system)
    system.actorOf(PersistenceHistograms.props(), "persistence-histogram-printer")
  }

}
