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

import scala.concurrent.duration._
import scala.concurrent._

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import akka.cluster.pubsub._


object Driver {
  case object CollectResults

  def props(numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int): Props =
    Props(new Driver(numberOfTopics, numberOfPublishers, numberOfSubscribers))
}
class Driver(numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int) extends Actor with ActorLogging {
  import Driver._

  implicit val ec: ExecutionContext = context.dispatcher
  val messagesPerPublisher = 1000

  val mediator = DistributedPubSub(context.system).mediator

  val subscribers = (0 until numberOfSubscribers).map(n => context.actorOf(Subscriber.props(n % numberOfTopics, mediator)))
  val publishers = (0 until numberOfPublishers).map(n => context.actorOf(Publisher.props(n % numberOfTopics)))

  val numberOfMessages = messagesPerPublisher * numberOfPublishers

  override def receive = subscribing(subscribers.size)

  def subscribing(waitingForSubscribers: Int): Receive = {
    case Subscriber.Subscribed if waitingForSubscribers > 1 =>
      context.become(subscribing(waitingForSubscribers - 1))
    case Subscriber.Subscribed =>
      log.info("Subscribers initialized, publishing messages")
      publishers.foreach(publisher => {
        (0 until messagesPerPublisher).foreach(_ => publisher ! Publisher.Tick)
      })
      context.system.scheduler.scheduleOnce(10.seconds, self, CollectResults)
      context.become(waiting)
  }

  val waiting: Receive = {
    case CollectResults =>
      implicit val timeout: Timeout = 5.seconds
      Future.sequence(subscribers.map(_ ? Subscriber.CollectStats))
        .pipeTo(self)
    case results: Seq[_] =>
      println(s"Published $numberOfMessages messages, ${results.asInstanceOf[Seq[Int]].sum} arrived")
      context.system.terminate()
  }
}
