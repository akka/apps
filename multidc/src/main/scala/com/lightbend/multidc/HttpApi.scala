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

package com.lightbend.multidc

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.lightbend.multidc.ReplicatedCounter.{Get, Increment, ShardingEnvelope}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object HttpApi {

  def startServer(httpHost: String, httpPort: Int, counterProxy: ActorRef)(implicit system: ActorSystem) = {

    import akka.http.scaladsl.server.Directives._

    implicit val mat = ActorMaterializer()
    import system.dispatcher
    implicit val timeout: Timeout = Timeout(10.seconds)

    val api =
      path("counter") {
        get {
          parameters("id".as[String]) {
            (id: String) =>
              onComplete((counterProxy ? ShardingEnvelope(id, Get)).mapTo[Int]) {
                case Success(i) => complete(i.toString)
                case Failure(ex) => complete(StatusCodes.BadRequest, ex.toString)
              }
          }
        } ~ put {
          parameters("id".as[String]) {
            (id: String) => {
              counterProxy ! ShardingEnvelope(id, Increment("http request"))
              complete(StatusCodes.OK)
            }
          }
        }
      } ~
    path("bench") {
      get {
        parameters("counters".as[Int], "updates".as[Int]) {
          (counters, updates) =>
            (0 until counters).foreach(counter => {
              system.actorOf(Props(classOf[Incrementor], counter.toString, updates, counterProxy))
            })
            complete(StatusCodes.OK)
        }
      }
    }

    Http().bindAndHandle(api, httpHost, httpPort).onComplete {
      case Success(_) => system.log.info("HTTP Server bound to http://{}:{}", httpHost, httpPort)
      case Failure(ex) => system.log.error(ex, "Failed to bind HTTP Server to http://{}:{}", httpHost, httpPort)
    }

  }
}
