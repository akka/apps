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
import akka.cluster.pubsub._

object Publisher {
  def props(topic: Int, coordinator: ActorRef): Props = props(topic.toString, coordinator)
  def props(topic: String, coordinator: ActorRef): Props = Props(new Publisher(topic, coordinator))

  case class Started(publisher: ActorRef)
  case object Tick
}
class Publisher(topic: String, coordinator: ActorRef) extends Actor with ActorLogging {
  import Publisher._
  import DistributedPubSubMediator.Publish

  override def preStart() = {
//    log.info(s"Started publisher for $topic under $self")
    coordinator ! Started(self)
  }

  override def postStop() = log.info(s"Publisher stopped")

  // activate the extension
  val mediator = DistributedPubSub(context.system).mediator

  override def receive = {
    case Tick => mediator ! Publish(topic, Payload(42))
  }
}
