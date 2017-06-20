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
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import akka.cluster.pubsub._


object PubSubCoordinator {
  case object StartPublishing
  case object CollectResults

  def props(messagesPerPublisher: Int, numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int): Props =
    Props(new PubSubCoordinator(messagesPerPublisher, numberOfTopics, numberOfPublishers, numberOfSubscribers))
  def name = "pub-sub-coordinator"
}
class PubSubCoordinator(messagesPerPublisher: Int, numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int) extends Actor with ActorLogging {
  import PubSubCoordinator._

  implicit val ec: ExecutionContext = context.dispatcher

  val numberOfMessages = messagesPerPublisher * numberOfPublishers

  override def receive = subscribing(Set.empty, Set.empty, numberOfSubscribers, numberOfPublishers)

  def subscribing(subscribers: Set[ActorRef], publishers: Set[ActorRef], waitingForSubscribers: Int, waitingForPublishers: Int): Receive =
    if (waitingForSubscribers == 0 && waitingForPublishers == 0) {
      log.info(s"${subscribers.size} subscribers and ${publishers.size} publishers initialized, allowing for subscriber information to propagate")
      context.system.scheduler.scheduleOnce(20.seconds, self, StartPublishing)
      pausing(publishers, subscribers)
    } else {
    case Publisher.Started(publisher) =>
      context.become(subscribing(subscribers, publishers + publisher, waitingForSubscribers, waitingForPublishers - 1))
    case Subscriber.Subscribed =>
      context.become(subscribing(subscribers + sender(), publishers, waitingForSubscribers - 1, waitingForPublishers))
  }

  def pausing(publishers: Set[ActorRef], subscribers: Set[ActorRef]): Receive = {
    case StartPublishing =>
      log.info(s"Initiating publishing")
      publishers.foreach(publisher => {
        (0 until messagesPerPublisher).foreach(_ => publisher ! Publisher.Tick)
      })
      context.system.scheduler.scheduleOnce(10.seconds, self, CollectResults)
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
        context.system.terminate()
      }
  }
}
