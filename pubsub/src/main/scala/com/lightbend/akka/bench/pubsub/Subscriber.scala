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

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Random
import akka.actor._
import akka.cluster.pubsub._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import DistributedPubSubMediator.{Subscribe, SubscribeAck}

object Subscriber {
  def props(topic: Int, topicUntil: Int, mediator: ActorRef, coordinator: ActorRef): Props = Props(new Subscriber(topic, topicUntil, mediator, coordinator))

  case object Subscribed
  case object CollectStats
}
class Subscriber(topic: Int, topicUntil: Int, mediator: ActorRef, coordinator: ActorRef) extends Actor with ActorLogging {
  import Subscriber._

  var messagesReceived = 0
  val random = new Random()

  implicit val timeout: Timeout = 5.seconds
  implicit val ec: ExecutionContext = context.dispatcher

  Future.sequence((topic until topicUntil).map { n =>
    mediator ? Subscribe(n.toString, self)
  }).pipeTo(self)

  override def receive = {
    case _: Seq[_] ⇒
      log.info(s"Subscribed to $topic-$topicUntil")
      coordinator ! Subscribed
    case Payload(n) =>
      messagesReceived += 1
    case CollectStats =>
      // log.info("Collecting stats from this subscriber")
      sender() ! messagesReceived
  }
}
