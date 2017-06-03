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

package apps.pubsub

import akka.actor._
import akka.cluster.pubsub._
import DistributedPubSubMediator.{ Subscribe, SubscribeAck }


object Subscriber {
  def props(topic: Int, mediator: ActorRef): Props = props(topic.toString, mediator)
  def props(topic: String, mediator: ActorRef): Props = Props(new Subscriber(topic, mediator))

  case object Subscribed
  case object CollectStats
}
class Subscriber(topic: String, mediator: ActorRef) extends Actor {
  import Subscriber._

  var messagesReceived = 0

  mediator ! Subscribe(topic, self)

  override def receive = {
    case SubscribeAck(Subscribe(topic, None, `self`)) ⇒
      // log.info("subscribed")
      context.parent ! Subscribed
    case Payload(n) =>
      // log.info(s"Got message $n")
      messagesReceived += 1
    case CollectStats =>
      sender() ! messagesReceived
  }
}
