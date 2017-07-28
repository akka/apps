package com.lightbend.akka.bench.sharding

import java.io.File
import java.net.InetAddress

import com.typesafe.config.{ Config, ConfigFactory }

object BenchmarkConfig {
  def load(): Config = {
    val bindAddressConf = ConfigFactory.parseString(
      s"""
         akka {
           cluster.http.management.hostname = "${InetAddress.getLocalHost.getHostAddress}"

           remote {
             artery.canonical.hostname = "${InetAddress.getLocalHost.getHostAddress}"
           }
         }
        """)

    val rootConfFile = new File("/home/akka/root-application.conf")
    val rootConf =
      if (rootConfFile.exists) ConfigFactory.parseFile(rootConfFile)
      else ConfigFactory.empty("no-root-application-conf-found")

    bindAddressConf
      .withFallback(rootConf
        .withFallback(ConfigFactory.load()))
  }

}
