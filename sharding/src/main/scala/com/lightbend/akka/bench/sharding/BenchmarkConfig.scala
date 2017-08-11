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

    ConfigFactory.load(bindAddressConf.withFallback(rootConf).withFallback(ConfigFactory.defaultApplication()))
  }

}
