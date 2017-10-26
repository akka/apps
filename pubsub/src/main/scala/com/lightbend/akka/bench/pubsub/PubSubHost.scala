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

import akka.actor._
import akka.cluster.singleton._

object PubSubHost {

  case class StartSession(sessionId: Int, numberOfHosts: Int, numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int)
  case class StopRun(runId: Int)

  def props(): Props = Props(new PubSubHost())
  val Name = "pub-sub-host"

}

/**
 * One of these runs on each node in the cluster
 */
class PubSubHost() extends Actor with ActorLogging {

  import PubSubHost._

  log.info("PubSubHost started")

  val coordinator = context.actorOf(
    ClusterSingletonProxy.props(
      singletonManagerPath = "/user/" + PubSubBenchmark.CoordinatorManager,
      settings = ClusterSingletonProxySettings(context.system).withRole("master")),
    name = "coordinatorProxy")

  def receive = idle

  def idle: Receive = {
    case StartSession(id, numberOfHosts, numberOfTopics, numberOfPublishers, numberOfSubscribers) =>
      val runner = context.watch(context.actorOf(PubSubHostSession.props(id, sender(), numberOfHosts, numberOfTopics, numberOfPublishers, numberOfSubscribers), s"session-$id"))
      context.become(running(runner, id))

  }

  def running(runner: ActorRef, sessionId: Int): Receive = {
    case Terminated(_) =>
      log.info("Host run {} stopped itself", sessionId)
      context.become(idle)

    case StopRun(`sessionId`) =>
      context.unwatch(runner)
      runner ! PubSubHostSession.Stop(sessionId)
      log.info("Host run {} completed from coordinator", sessionId)
      context.become(idle)
  }
}