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

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.{ ActorSystem, Props }
import akka.pattern.ask
import akka.cluster.Cluster
import akka.cluster.http.management.ClusterHttpManagement
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

object InitCassandraApp extends App {

  // setup for clound env -------------------------------------------------------------
  val rootConfFile = new File("/home/akka/root-application.conf")
  val rootConf =
    if (rootConfFile.exists) ConfigFactory.parseFile(rootConfFile)
    else ConfigFactory.empty("no-root-application-conf-found")

  val conf = rootConf.withFallback(ConfigFactory.load())
  // end of setup for clound env ------------------------------------------------------ 

  val systemName = Try(conf.getString("akka.system-name")).getOrElse("DistributedDataSystem")
  implicit val system = ActorSystem(systemName, conf)

  // management -----------
  val cluster = Cluster(system)
  ClusterHttpManagement(cluster).start()
  // end of management ----

  implicit val timeout = Timeout(45.seconds)

  val journalPluginId = ""
  val snapshotPluginId = ""

  val t0 = System.nanoTime()
  val initActor = system.actorOf(Props(classOf[AwaitPersistenceInit], journalPluginId, snapshotPluginId), "persistenceInit")
  val reply = initActor ? "hello"
  Await.ready(reply, 60.seconds)
  system.log.debug("awaitPersistenceInit took {} ms {}", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0), system.name)

}

class AwaitPersistenceInit(
  override val journalPluginId: String,
  override val snapshotPluginId: String
) extends PersistentActor {
  def persistenceId: String = "persistenceInit"

  def receiveRecover: Receive = {
    case _ =>
  }

  def receiveCommand: Receive = {
    case msg =>
      persist(msg) { _ =>
        sender() ! msg
        context.stop(self)
      }
  }
}
