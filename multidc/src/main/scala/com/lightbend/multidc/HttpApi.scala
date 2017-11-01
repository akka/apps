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
import akka.http.scaladsl.{Http, model}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.pattern.ask
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.{ByteString, Timeout}
import com.lightbend.multidc.ReplicatedCounter.{Get, Increment, ShardingEnvelope}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object HttpApi {

  def startServer(httpHost: String, httpPort: Int, counterProxy: ActorRef, introspectorProxy: ActorRef)(implicit system: ActorSystem) = {

    import akka.http.scaladsl.server.Directives._

    implicit val mat = ActorMaterializer()
    import system.dispatcher
    implicit val timeout: Timeout = Timeout(10.seconds)

    val counter = path("counter") {
      concat(
        get {
          parameters("id") { id ⇒
            onComplete((counterProxy ? ShardingEnvelope(id, Get)).mapTo[Int]) {
              case Success(i) => complete(i.toString)
              case Failure(ex) => complete(StatusCodes.BadRequest, ex.toString)
            }
          }
        },
        put {
          parameters("id") { id ⇒
            counterProxy ! ShardingEnvelope(id, Increment("http request"))
            complete(StatusCodes.OK)
          }
        }
      )
    }

    // these routes are not "RESTful" and I don't care :P Optimised for easy use from CLI.
    val introspector = pathPrefix("introspector") {
      concat(
        path(Segment) { id ⇒
          val refSource = Source.actorRef[ReplicatedIntrospector.Event](Int.MaxValue, OverflowStrategy.fail)
            .map(_.render)

          // TODO "pre-materialize pattern"
          val (ref, pub) =refSource.toMat(Sink.asPublisher(true))(Keep.both).run()
          val s = Source.fromPublisher(pub)
            .intersperse("\n")
            .map(ByteString(_))
          // end of "pre-materialize pattern"

          introspectorProxy.tell(ReplicatedIntrospector.Inspect, ref)

          complete(HttpResponse(entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, s)))
        } ~
        path(Segment / "write" / Segment) { (id, data) ⇒
          onComplete((introspectorProxy ? ReplicatedIntrospector.Inspect).mapTo[Vector[ReplicatedIntrospector.Event]]) { events ⇒
            val s = Source(events.get)
              .map(_.render)
              .intersperse("\n")
              .map(ByteString(_))

            complete(HttpResponse(entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, s)))
          }
        }
      )
    }

    val api = concat(
      counter,
      introspector,

      path("test") {
        get {
          parameters("counters".as[Int], "updates".as[Int]) { (counters, updates) =>
            (0 until counters).foreach(counter => {
              system.actorOf(Props(classOf[Incrementor], counter.toString, updates, counterProxy))
            })
            complete(StatusCodes.OK)
          }
        }
      },

      path("single-counter-test") {
        get {
          parameters("counter".as[String], "updates".as[Int]) {
            (counter, updates) =>
              system.actorOf(Props(classOf[Incrementor], counter, updates, counterProxy))
              complete(StatusCodes.OK)
          }
        }
      }
    )

    Http().bindAndHandle(api, httpHost, httpPort).onComplete {
      case Success(_) => system.log.info("HTTP Server bound to http://{}:{}", httpHost, httpPort)
      case Failure(ex) => system.log.error(ex, "Failed to bind HTTP Server to http://{}:{}", httpHost, httpPort)
    }

  }
}
