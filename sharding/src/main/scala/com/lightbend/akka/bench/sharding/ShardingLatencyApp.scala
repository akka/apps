/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.typesafe.com>
 */
package com.lightbend.akka.bench.sharding

import java.io.File

import akka.actor.{ ActorSystem, Props }
import com.typesafe.config.ConfigFactory

object ShardingLatencyApp extends App {

  // TODO only a single node must be the "bench" role
  val role = if (false) "bench" else "shard" // FIXME this won't fly, make it check if it is leader?
  println(s"NODE STARTING WITH ROLE: $role")


  // setup for clound env -------------------------------------------------------------
    val rootConfFile = new File("/home/akka/root-application.conf")
    val rootConf = 
      if (rootConfFile.exists) ConfigFactory.parseFile(rootConfFile) 
      else ConfigFactory.empty("no-root-application-conf-found")
    
    val conf = rootConf.withFallback(ConfigFactory.load())
    // end of setup for clound env ------------------------------------------------------ 
  
    val systemName = Option(conf.getString("akka.system-name")).getOrElse("DistributedDataSystem")
    implicit val system = ActorSystem(systemName, conf)
  
  if (role == "bench") {
    system.actorOf(Props[PingLatencyCoordinator], "bench-coordinator")
  } else {
    BenchEntity.startRegion(system)
    system.actorOf(PersistenceHistograms.props(), "persistence-histogram-printer")
  }

}
