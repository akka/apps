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
import akka.cluster.singleton._
import akka.util.Timeout
import akka.cluster.pubsub._

object PubSubHost {
  def props(numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int): Props =
    Props(new PubSubHost(numberOfTopics, numberOfPublishers, numberOfSubscribers))
  def name = "pub-sub-host"
}
class PubSubHost(numberOfTopics: Int, numberOfPublishers: Int, numberOfSubscribers: Int) extends Actor {
  val NumNodes = context.system.settings.config.getInt("akka.cluster.min-nr-of-members")

  val mediator = DistributedPubSub(context.system).mediator

  val coordinator = context.actorOf(
    ClusterSingletonProxy.props(
      singletonManagerPath = "/user/" + PubSubBenchmark.CoordinatorManager,
      settings = ClusterSingletonProxySettings(context.system)),
    name = "coordinatorProxy")

  val subscribersOnThisNode = numberOfSubscribers / NumNodes
  val publishersOnThisNode = numberOfPublishers / NumNodes

  val subscribers = (0 until subscribersOnThisNode).map(n => context.actorOf(Subscriber.props(n % numberOfTopics, mediator, coordinator), s"subscriber-$n"))
  val publishers = (0 until publishersOnThisNode).map(n => context.actorOf(Publisher.props(Math.min(subscribersOnThisNode, numberOfTopics), coordinator), s"publisher-$n"))

  def receive = {
    case _ => ???
  }
}