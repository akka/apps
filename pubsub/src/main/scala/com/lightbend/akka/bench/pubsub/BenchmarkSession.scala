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

import scala.concurrent.duration._
import scala.concurrent._
import akka.actor._
import akka.cluster.{Cluster, MemberStatus}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import akka.cluster.pubsub._

object BenchmarkSession {
  case object StartPublishing
  case object CollectResults

  def props(runId: Int, numberOfNodes: Int, messagesPerPublisher: Int, numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int): Props =
    Props(new BenchmarkSession(runId: Int, numberOfNodes: Int, messagesPerPublisher, numberOfTopics, numberOfPublishers, numberOfSubscribers))
  val Name = "pub-sub-coordinator"
}

/**
 * Abstracts coordination running one benchmark on the master node, stops itself when done
 */
class BenchmarkSession(sessionId: Int, numberOfNodes: Int, messagesPerPublisher: Int, numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int) extends Actor with ActorLogging {
  import BenchmarkSession._
  require(numberOfSubscribers <= numberOfPublishers || numberOfSubscribers <= numberOfTopics)

  log.info("Starting bench run id {}", sessionId)

  implicit val ec: ExecutionContext = context.dispatcher

  val started = System.nanoTime()
  val numberOfMessages = messagesPerPublisher * numberOfPublishers

  val participantHosts = Cluster(context.system).state.members.collect {
    case m if m.status == MemberStatus.Up =>
      context.actorSelection(RootActorPath(m.address) / "user" / PubSubHost.Name)
  }.take(numberOfNodes)

  if (participantHosts.size < numberOfNodes) {
    log.warning("Was requested to run on {} nodes, but cluster doesn't contain that many UP nodes, bailing out of testrun", numberOfNodes)
    context.stop(self)
  }

  participantHosts.foreach { host =>
    host ! PubSubHost.StartSession(sessionId, numberOfNodes, numberOfTopics, numberOfPublishers, numberOfSubscribers)
  }

  override def receive = subscribing(Set.empty, Set.empty, numberOfSubscribers, numberOfPublishers)

  def subscribing(subscribers: Set[ActorRef], publishers: Set[ActorRef], waitingForSubscribers: Int, waitingForPublishers: Int): Receive =
    if (waitingForSubscribers == 0 && waitingForPublishers == 0) {
      val backoff = 10.seconds
      log.info(s"${subscribers.size} subscribers and ${publishers.size} publishers initialized, allowing for subscriber information to propagate for $backoff")
      context.system.scheduler.scheduleOnce(10.seconds, self, StartPublishing)
      pausing(publishers, subscribers)
    } else {
      case Publisher.Started(`sessionId`, publisher) =>
        log.info("Got publisher: " + publisher)
        context.become(subscribing(subscribers, publishers + publisher, waitingForSubscribers, waitingForPublishers - 1))
      case Subscriber.Subscribed(`sessionId`) =>
        log.info("Got subscriber: " + sender())
        context.become(subscribing(subscribers + sender(), publishers, waitingForSubscribers - 1, waitingForPublishers))
    }

  def pausing(publishers: Set[ActorRef], subscribers: Set[ActorRef]): Receive = {
    case StartPublishing =>
      log.info(s"Initiating publishing")
      publishers.foreach(_ ! Publisher.Start(sessionId, messagesPerPublisher))
      context.system.scheduler.scheduleOnce(20.seconds, self, CollectResults)
      context.become(waiting(subscribers))
  }

  def waiting(subscribers: Set[ActorRef]): Receive = {
    case CollectResults =>
      implicit val timeout: Timeout = 30.seconds
      log.info(s"Collecting results from ${subscribers.size} subscribers")
      subscribers
        .map(_ ? Subscriber.CollectStats)
        .map(_.pipeTo(self))
      context.become(waitingFor(subscribers.size))
  }

  def waitingFor(subscribers: Int, resultSoFar: Int = 0): Receive = {
    case i: Int =>
      if (subscribers > 1) context.become(waitingFor(subscribers - 1, resultSoFar + i))
      else {
        log.info(s"Published $numberOfMessages messages, ${resultSoFar+i} arrived")
        val timing = (System.nanoTime() - started) / 1000000
        val result = BenchmarkCoordinator.BenchResult(
          sessionId,
          numberOfNodes,
          messagesPerPublisher,
          numberOfTopics,
          numberOfPublishers,
          numberOfSubscribers,
          timing.millis,
          resultSoFar,
          numberOfMessages)
        context.parent ! result
        participantHosts.foreach(_ ! PubSubHost.StopRun(sessionId))
        context.stop(self)
      }
  }
}
