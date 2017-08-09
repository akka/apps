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
import scala.util.Random
import akka.actor._
import akka.cluster.pubsub._
import DistributedPubSubMediator.{Subscribe, SubscribeAck}
import scala.concurrent.duration._

object Subscriber {
  def props(runId: Int, topic: Int, topicUntil: Int, mediator: ActorRef, coordinator: ActorRef): Props =
    Props(new Subscriber(runId, topic, topicUntil, mediator, coordinator))

  case class Subscribed(runId: Int)
  case object CollectStats
  case object SubscriptionTimedOut
  case object SubscribeNextBatch
}
class Subscriber(runId: Int, topic: Int, topicUntil: Int, mediator: ActorRef, coordinator: ActorRef) extends Actor with ActorLogging {
  import Subscriber._

  var messagesReceived = 0
  val random = new Random()

  val SubscriptionTimeout = 10.seconds
  val SubscribeBatchSize = 100
  import context.dispatcher

  var topicAcksLeft = topicUntil - topic
  var subscriptionsInFlight = 0
  (topic until topicUntil).foreach { n =>
    mediator ! Subscribe(n.toString, self)
  }

  override def receive = subscribing()

  def subscribing(): Receive = {
    // to not overwhelm the mediator we subscribe in batches
    var subscribingTask: Option[Cancellable] = None
    def scheduleNextSubscription(): Unit =
      subscribingTask = Some(context.system.scheduler.scheduleOnce(10.millis, self, SubscribeNextBatch))
    val subscriptionTimeoutTask = context.system.scheduler.scheduleOnce(SubscriptionTimeout, self, SubscriptionTimedOut)
    val subscriptions = (topic until topicUntil).iterator
    self ! SubscribeNextBatch

    {
      case _: SubscribeAck =>
        topicAcksLeft -= 1
        subscriptionsInFlight -= 1
        if (topicAcksLeft == 0) {
          log.info(s"Subscribed to topics $topic-$topicUntil")
          context.become(subscribed)
          subscribingTask.foreach(_.cancel())
          subscriptionTimeoutTask.cancel()
          coordinator ! Subscribed(runId)
        }

      case SubscribeNextBatch =>
        val numberOfNewSubscriptions = SubscribeBatchSize - subscriptionsInFlight
        if (numberOfNewSubscriptions > 0) {
          subscriptions.take(numberOfNewSubscriptions).foreach { n =>
            mediator ! Subscribe(n.toString, self)
          }
          subscriptionsInFlight += numberOfNewSubscriptions
        }
        if (subscriptions.hasNext) scheduleNextSubscription()


      case SubscriptionTimeout =>
        log.error("Subscriptions timed out")
        subscribingTask.foreach(_.cancel())
        context.stop(self)

    }
  }

  def subscribed: Receive = {
    case Payload(_) =>
      messagesReceived += 1
    case CollectStats =>
      // log.info("Collecting stats from this subscriber")
      sender() ! messagesReceived
  }
}
