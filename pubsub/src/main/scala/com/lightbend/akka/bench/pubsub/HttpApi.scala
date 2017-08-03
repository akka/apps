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

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.Cluster
import akka.cluster.http.management.ClusterHttpManagementRoutes
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

import scala.util.{Failure, Success}
object HttpApi {

  def startServer(host: String, port: Int, coordinator: ActorRef)(implicit system: ActorSystem) = {

    implicit val mat = ActorMaterializer()
    import system.dispatcher

    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import spray.json._
    import DefaultJsonProtocol._
    implicit object FiniteDurationFormat extends JsonFormat[FiniteDuration] {
      def write(x: FiniteDuration) = JsString(x.toString())
      def read(value: JsValue) = value match {
        case x => deserializationError("I know nothing about reading" + x)
      }
    }
    implicit val benchResultFormat = jsonFormat9(BenchmarkCoordinator.BenchResult)
    implicit val resultFormat = jsonFormat1(BenchmarkCoordinator.AllResults)

    val routes = concat(
      path("bench") {
        post {
          parameters('numberOfNodes.as[Int], 'messagesPerPublisher.as[Int], 'numberOfTopics.as[Int], 'numberOfPublishers.as[Int], 'numberOfSubscribers.as[Int]) {
            (numberOfNodes, messagesPerPublisher, numberOfTopics, numberOfPublishers, numberOfSubscribers) =>
              implicit val timeout: Timeout = 5.seconds
              onSuccess(coordinator ? BenchmarkCoordinator.StartRun(numberOfNodes, messagesPerPublisher, numberOfTopics, numberOfPublishers, numberOfSubscribers)) {
                case BenchmarkCoordinator.RunInitialized(id) => complete(s"run $id started")
              }
          }
        } ~
        get {
          implicit val timeout: Timeout = 5.seconds
          onSuccess(coordinator ? BenchmarkCoordinator.GetResults) {
            case result: BenchmarkCoordinator.AllResults => complete(result)
          }

        }
      },
      ClusterHttpManagementRoutes(Cluster(system))
    )

    Http().bindAndHandle(routes, host, port).onComplete {
      case Success(_) => system.log.info("HTTP server running at {}:{}", host, port)
      case Failure(ex) => system.log.error(ex, "Failed to start HTTP server")
    }
  }
}
