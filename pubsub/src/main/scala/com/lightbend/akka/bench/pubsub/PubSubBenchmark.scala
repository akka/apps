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

import akka.actor.{ ActorSystem, PoisonPill, Props }
import akka.cluster.Cluster
import java.io.File

import akka.cluster.singleton.{ ClusterSingletonManager, ClusterSingletonManagerSettings }
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
  val system = ActorSystem(systemName, conf)

  Cluster(system).registerOnMemberUp {
    val numberOfPublishers = 50
    val numberOfSubscribers = 50
    val messagesPerPublisher = 1000

    val numberOfTopics = 50000
    require(numberOfSubscribers <= numberOfPublishers || numberOfSubscribers <= numberOfTopics)

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = PubSubCoordinator.props(messagesPerPublisher = messagesPerPublisher, numberOfTopics = numberOfTopics, numberOfPublishers = numberOfPublishers, numberOfSubscribers = numberOfSubscribers),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system)),
      name = CoordinatorManager)

    system.actorOf(PubSubHost.props(numberOfTopics = numberOfTopics, numberOfPublishers = numberOfPublishers, numberOfSubscribers = numberOfSubscribers), PubSubHost.name)
  }

  scala.io.StdIn.readLine() // TODO not sure if readline will work well with starting it via scripts...
  system.terminate()
}
