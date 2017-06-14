/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.typesafe.com>
 */
package com.lightbend.akka.bench.sharding

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId}

import scala.concurrent.duration._

object BenchSettings extends ExtensionId[BenchSettings] {
  override def createExtension(system: ExtendedActorSystem): BenchSettings =
    new BenchSettings(system)
}

class BenchSettings(system: ActorSystem) extends Extension {
  private val config = system.settings.config.getConfig("shard-bench")
  val UniqueEntities = config.getInt("unique-entities")
  val NumberOfPings = config.getInt("number-of-pings")
  val PingsPerSecond = config.getInt("pings-per-second")
  val NumberOfShards = config.getInt("number-of-shards")
  val TotalNodes = config.getInt("total-nodes")
}
