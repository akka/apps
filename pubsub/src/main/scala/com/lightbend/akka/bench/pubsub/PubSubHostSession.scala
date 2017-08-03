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

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.cluster.pubsub.DistributedPubSub

object PubSubHostSession {

  case class Stop(runId: Int)

  def props(runId: Int, coordinator: ActorRef, numberOfNodes: Int, numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int): Props =
    Props(new PubSubHostSession(runId, coordinator, numberOfNodes, numberOfTopics, numberOfPublishers, numberOfSubscribers))
}

class PubSubHostSession(sessionId: Int, coordinator: ActorRef, numberOfNodes: Int, numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int) extends Actor with ActorLogging {
  import PubSubHostSession._
  log.info("Starting host session {}", sessionId)

  val mediator = DistributedPubSub(context.system).mediator

  val subscribersOnThisNode = numberOfSubscribers / numberOfNodes
  val publishersOnThisNode = numberOfPublishers / numberOfNodes

  val topicsPerSubscriber = numberOfTopics / subscribersOnThisNode

  (0 until numberOfTopics by topicsPerSubscriber).foreach(n =>
    context.watch(context.actorOf(Subscriber.props(sessionId, n, Math.min(n + topicsPerSubscriber, numberOfTopics), mediator, coordinator), s"subscriber-$n"))
  )

  val publishers = (0 until publishersOnThisNode).foreach(n =>
    context.watch(context.actorOf(Publisher.props(sessionId, numberOfTopics, coordinator), s"publisher-$n")))


  def receive = {
    case Stop(`sessionId`) =>
      log.info("Shutting down session {}", sessionId)
      context.stop(self)

    case Terminated(ref) =>
      if (context.children.isEmpty) {
        log.info("All publishers and subscribers shut down for session {}", sessionId)
        context.stop(self)
      }

  }
}