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

import akka.actor.{Actor, ActorLogging, Props, Terminated}

object BenchmarkCoordinator {
  case class StartRun(numberOfNodes: Int, messagesPerPublisher: Int, numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int) {
    require(numberOfPublishers % numberOfNodes == 0, "Number of publishers must be evenly divisible over the number of nodes")
    require(numberOfSubscribers % numberOfNodes == 0, "Number of subscribers must be evenly divisible over the number of nodes")
    require(numberOfSubscribers <= numberOfTopics, "Number of subscribers cannot be more than the number of topics")
    require(numberOfTopics % numberOfSubscribers == 0, "Number of topics must be evenly divisible over the number of subscribers")
  }
  case class RunInitialized(id: Int)

  case class BenchResult(
    id: Int,
    numberOfNodes: Int,
    messagesPerPublisher: Int,
    numberOfTopics: Int,
    numberOfPublishers: Int,
    numberOfSubscribers: Int,
    messagesArrived: Int,
    failed: Int)

  case object GetResults
  case class AllResults(results: List[BenchResult])

  def props(): Props = Props(new BenchmarkCoordinator())
}

class BenchmarkCoordinator extends Actor with ActorLogging {

  import BenchmarkCoordinator._

  var _runId = 0
  def nextRunId() = {
    _runId += 1
    _runId
  }

  var results = List.empty[BenchResult]

  def receive = idle

  def idle: Receive = {
    case StartRun(numberOfNodes, msgPerP, numTopic, numPubs, numSubs) =>
      val id = nextRunId()
      context.watch(context.actorOf(BenchmarkSession.props(id, numberOfNodes, msgPerP, numTopic, numPubs, numSubs), s"run-$id"))
      sender() ! RunInitialized(id)
      context.become(running)

    case GetResults => sender() ! AllResults(results)

  }

  def running: Receive = {

    case GetResults => sender() ! AllResults(results)

    case result: BenchResult =>
      results = result :: results

    case Terminated(_) =>
      log.info("Run terminated")
      context.become(idle)
  }

}
