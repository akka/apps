package testapp

import java.net.InetAddress
import scala.collection.immutable
import org.slf4j.LoggerFactory
import com.amazonaws.services.opsworks.AWSOpsWorksClient
import com.amazonaws.services.opsworks.model.DescribeInstancesRequest
import com.amazonaws.services.opsworks.model.Instance
import com.typesafe.config.ConfigFactory

import akka.actor._
import akka.cluster.Cluster
import akka.contrib.pattern.ClusterSingletonManager
import akka.kernel.Bootable

class Boot extends Bootable {
  val log = LoggerFactory.getLogger(getClass)
  var system: ActorSystem = _

  def startup(): Unit = {
    val stackId = System.getProperty("ops-stack-id")
    val selfHostName = InetAddress.getLocalHost.getHostName
    val (selfIp, seedNodesStr) = {
      if (stackId eq null)
        (InetAddress.getLocalHost.getHostAddress, "")
      else {
        val instances = opsInstances(stackId).sortBy(_.getHostname)
        val ips = instances.take(5).map { i ⇒
          if (i.getPrivateIp eq null) i.getHostname // not started, but should still be in the seed-nodes
          else i.getPrivateIp
        }
        val selfIp = instances.collectFirst { case i if (i.getPrivateIp ne null) && (i.getHostname == selfHostName) ⇒ i.getPrivateIp }
        require(selfIp.nonEmpty, s"Couldn't find my own [${selfHostName}] private ip in list of instances [${instances}]")
        val seedNodesStr = ips.map("akka.tcp://TestApp@" + _ + ":2552").mkString("\"", "\",\"", "\"")
        (selfIp.get, seedNodesStr)
      }
    }
    log.info(s"[${selfHostName}/${selfIp}] starting with seed-nodes=[${seedNodesStr}]")

    val conf = ConfigFactory.parseString(s"""
        akka.remote.netty.tcp.hostname="${selfIp}"
        akka.cluster.seed-nodes=[${seedNodesStr}]
        """).withFallback(ConfigFactory.load)

    system = ActorSystem("TestApp", conf)
    val cluster = Cluster(system)

    system.actorOf(Props[MemberListener], name = "members")
    system.actorOf(Props[MetricsListener], name = "metrics")

    system.actorOf(ClusterSingletonManager.props(
      singletonProps = _ ⇒ Props[StatsService], singletonName = "singleton",
      terminationMessage = PoisonPill, role = Some("backend")),
      name = "stats")

    if (cluster.selfRoles.contains("frontend"))
      cluster.registerOnMemberUp {
        system.actorOf(Props[StatsClient], "statsClient")
      }

  }

  def opsInstances(stackId: String): immutable.IndexedSeq[Instance] = {
    try {
      import scala.collection.JavaConverters._
      val client = new AWSOpsWorksClient
      val req = (new DescribeInstancesRequest).withStackId(stackId)
      val result = client.describeInstances(req)
      result.getInstances.asScala.toVector
    } catch {
      case e: Exception ⇒
        log.warn("OpsWorks not available, due to: {}", e.getMessage)
        Vector.empty
    }
  }

  def shutdown: Unit = {
    if (system ne null) {
      system.shutdown()
      system = null
    }
  }

}

object Main {
  def main(args: Array[String]): Unit = {
    (new Boot).startup()
  }
}

