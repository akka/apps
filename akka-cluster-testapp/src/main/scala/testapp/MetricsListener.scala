package testapp

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.ClusterMetricsChanged
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.NodeMetrics
import akka.cluster.StandardMetrics.Cpu
import akka.cluster.StandardMetrics.HeapMemory

class MetricsListener extends Actor with ActorLogging {
  val cluster = Cluster(context.system)
  val selfAddress = cluster.selfAddress

  override def preStart(): Unit =
    cluster.subscribe(self, classOf[ClusterMetricsChanged])

  override def postStop(): Unit =
    cluster.unsubscribe(self)

  var t = System.nanoTime

  def receive = {
    case ClusterMetricsChanged(clusterMetrics) ⇒
      // FIXME we should publish ClusterMetricsChanged from Cluster periodically, instead of when it is received
      val now = System.nanoTime
      if ((now - t) >= 1000000000L) {
        t = now
        clusterMetrics.filter(_.address == selfAddress) foreach { nodeMetrics ⇒
          logHeap(nodeMetrics)
          logCpu(nodeMetrics)
        }
      }
    case state: CurrentClusterState ⇒ // ignore
  }

  def logHeap(nodeMetrics: NodeMetrics): Unit = nodeMetrics match {
    case HeapMemory(address, timestamp, used, committed, max) ⇒
      log.info("Used heap: [{}] MB", (used.doubleValue / 1024 / 1024).toInt)
    case _ ⇒ // no heap info
  }

  def logCpu(nodeMetrics: NodeMetrics): Unit = nodeMetrics match {
    case Cpu(address, timestamp, Some(systemLoadAverage), cpuCombined, processors) ⇒
      log.info("Load: [{}], cpu: [{}] ([{}] processors)", systemLoadAverage, cpuCombined.getOrElse("N/A"), processors)
    case _ ⇒ // no cpu info
  }
}
