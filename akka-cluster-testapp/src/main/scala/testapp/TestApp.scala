package testapp

import akka.actor.ActorSystem
import akka.actor.Props
import akka.contrib.pattern.ClusterSingletonManager
import akka.cluster.Cluster
import akka.actor.PoisonPill

object TestApp {
  def start(system: ActorSystem): Unit = {
    val cluster = Cluster(system)
    val conf = system.settings.config

    system.actorOf(ClusterSingletonManager.props(
      singletonProps = Props[ResultCollector], singletonName = "singleton",
      terminationMessage = PoisonPill, role = None),
      name = "resultsCollector")

    system.actorOf(Props[MemberListener], name = "members")
    if (cluster.settings.MetricsEnabled)
      system.actorOf(Props[MetricsListener], name = "metrics")

    val factorialEnabled = conf.getBoolean("factorial.enabled")

    if (cluster.selfRoles.contains("backend")) {
      system.actorOf(ClusterSingletonManager.props(
        singletonProps = Props[StatsService], singletonName = "service",
        terminationMessage = PoisonPill, role = Some("backend")),
        name = "statsBackend")

      if (factorialEnabled)
        system.actorOf(Props[FactorialBackend], name = "factorialBackend")
    }

    if (cluster.selfRoles.contains("frontend"))
      cluster.registerOnMemberUp {
        system.actorOf(Props[StatsClient], "statsClient")

        if (factorialEnabled) {
          val n = conf.getInt("factorial.n")
          val batchSize = conf.getInt("factorial.batch-size")
          system.actorOf(ClusterSingletonManager.props(
            singletonProps = Props(classOf[FactorialFrontend], n, batchSize), singletonName = "producer",
            terminationMessage = PoisonPill, role = Some("frontend")),
            name = "factorialFrontend")
        }
      }

  }
}