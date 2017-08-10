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

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId}

import scala.concurrent.duration._

object BenchSettings extends ExtensionId[BenchSettings] {
  override def createExtension(system: ExtendedActorSystem): BenchSettings =
    new BenchSettings(system)
}

sealed trait ShardingBenchmarkMode
case object PersistShardingBenchmark extends ShardingBenchmarkMode
case object RawPingPongShardingBenchmark extends ShardingBenchmarkMode

class BenchSettings(system: ActorSystem) extends Extension {
  val warmupAll: Boolean = config.getBoolean("warmup-all")

  private val config = system.settings.config.getConfig("shard-bench")
  val Mode = config.getString("mode") match {
      case "persist" => PersistShardingBenchmark
    case _ => RawPingPongShardingBenchmark
  }
  val UniqueEntities = config.getInt("unique-entities")
//  val NumberOfPings = config.getInt("number-of-pings")
  val PingsPerSecond = config.getInt("pings-per-second")
  val NumberOfShards = config.getInt("number-of-shards")
  val MinimumNodes = config.getInt("minimum-nodes")
  
  val AddActorsPerBatch = config.getInt("add-actors-batch")
  val AddActorsInterval = config.getDuration("add-actors-interval", TimeUnit.SECONDS).seconds
}
