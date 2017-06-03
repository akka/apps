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

package apps

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}

object DistributedDataBenchmark extends App {

  final val CoordinatorManager = "coordinatorManager"

  val system = ActorSystem("DistributedDataBenchmark")

  Cluster(system).registerOnMemberUp {
    system.actorOf(Props[DDataHost], DDataHost.Name)

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props[DDataBenchmarkCoordinator],
        terminationMessage = DDataBenchmarkCoordinator.End,
        settings = ClusterSingletonManagerSettings(system)),
      name = CoordinatorManager)
  }

  scala.io.StdIn.readLine()
  system.terminate()
}
