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

import scala.util.Random
import scala.concurrent.duration._
import akka.actor._
import akka.cluster.pubsub._

import scala.concurrent.ExecutionContext

object Publisher {
  def props(runId: Int, maxTopic: Int, coordinator: ActorRef): Props = Props(new Publisher(runId, maxTopic, coordinator))

  case class Started(runId: Int, publisher: ActorRef)
  case class Start(runId: Int, messagesToPublish: Int)
  case object Tick
}
class Publisher(runId: Int, maxTopic: Int, coordinator: ActorRef) extends Actor with ActorLogging {
  import Publisher._
  import DistributedPubSubMediator.Publish

  implicit val ec: ExecutionContext = context.dispatcher

  override def preStart() = {
    coordinator ! Started(runId, self)
  }

  val mediator = DistributedPubSub(context.system).mediator
  val random = new Random()

  override def receive = {
    case Start(`runId`, messagesToPublish) =>
      log.debug("publisher starting to publish")
      context.become(ticking(messagesToPublish))
  }

  def ticking(messagesToPublish: Int): Receive = {
    var task: Option[Cancellable] = None
    var ticksLeft = messagesToPublish
    self ! Tick

    {
      case Tick =>
        mediator ! Publish(random.nextInt(maxTopic).toString, Payload(42))
        if (ticksLeft == 1) {
          log.debug("Publisher done, shutting down")
          context.stop(self)
        } else {
          ticksLeft -= 1
          task = Some(context.system.scheduler.scheduleOnce(10.millis, self, Tick))
        }
    }
  }
}
