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

package com.lightbend.akka.bench.ddata

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.http.management.ClusterHttpManagementRoutes
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.{ByteString, Timeout}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object HttpApi {

  def startServer(httpHost: String, httpPort: Int)(implicit system: ActorSystem) = {

    import akka.http.scaladsl.server.Directives._

    implicit val mat = ActorMaterializer()
    import system.dispatcher

    val coordinator = system.actorOf(
      DDataBenchmarkCoordinator.singletonProxyProps(system),
      name = "coordinatorProxy")

    val api =
      concat(
        path("bench") {
          get {
            parameters("nodes".as[Int], "rounds".as[Int], "consistency".as[String], "writeTimeoutSecs".as[Int] ? 2) {
              (nodes, rounds, consistencyText, writeTimeout) =>
                implicit val timeout: Timeout = 10.seconds
                val consistency: WriteConsistency = consistencyText match {
                  case "local" => WriteLocal
                  case "majority" => WriteMajority(writeTimeout.seconds)
                  case "all" => WriteAll(writeTimeout.seconds)
                  case numNodes if numNodes.matches("\\d+") => WriteTo(numNodes.toInt, writeTimeout.seconds)
                }

                val source: Source[ByteString, Unit] =
                  Source.actorRef[DDataBenchmarkCoordinator.PartialOutput](100, OverflowStrategy.fail)
                    .mapMaterializedValue(outActor =>
                      coordinator ! DDataBenchmarkCoordinator.TriggerTest(nodes, rounds, consistency, outActor))
                    .map(partialOut => ByteString(partialOut.text + "\n"))

                // stream that output back
                complete(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, source))
            }
          }
        },
        ClusterHttpManagementRoutes(Cluster(system))
      )


    Http().bindAndHandle(api, httpHost, httpPort).onComplete {
      case Success(_) => system.log.info("HTTP Server bound to http://{}:{}", httpHost, httpPort)
      case Failure(ex) => system.log.error(ex, "Failed to bind HTTP Server to http://{}:{}", httpHost, httpPort)
    }

  }
}
