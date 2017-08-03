package com.lightbend.akka.bench.pubsub

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */
object HttpApi {

  def startServer(host: String, port: Int)(implicit system: ActorSystem) = {


    val routes = concat(
      path("bench") {
        post {
          parameters('whatever.as[Int]) { n =>
            complete("ok")
          }
        }
      }
    )
  }
}
