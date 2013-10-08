package testapp

import java.net.InetAddress
import org.slf4j.LoggerFactory
import com.amazonaws.services.opsworks.AWSOpsWorksClient
import com.amazonaws.services.opsworks.model.DescribeInstancesRequest
import com.amazonaws.services.opsworks.model.Instance
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.kernel.Bootable

/**
 * EC2 with OpsWorks deployment
 */
class OpsWorksBoot extends Bootable {
  val log = LoggerFactory.getLogger(getClass)
  var system: ActorSystem = _

  def startup(): Unit = {
    val instances = opsInstances()

    // bind the remoting to the IP address of this instance from the OpsWorks information
    val selfHostName = InetAddress.getLocalHost.getHostName
    val selfIp = instances.collectFirst { case i if i.getHostname == selfHostName ⇒ i.getPrivateIp } getOrElse {
      throw new IllegalArgumentException(s"Couldn't find my own [${selfHostName}] private ip in list of instances [${instances}]")
    }
    val conf = ConfigFactory.parseString(s"""
      akka.remote.netty.tcp.hostname="${selfIp}"
      """).withFallback(ConfigFactory.load)

    system = ActorSystem("TestApp", conf)
    val cluster = Cluster(system)

    // join some other node in the cluster
    // place this node first in the the list of seed nodes when it is tagged as first seed node
    val selfAddress = Cluster(system).selfAddress
    val otherNodes = instances.collect {
      case i if i.getPrivateIp != selfIp ⇒ selfAddress.copy(host = Some(i.getPrivateIp))
    }
    val seedNodes =
      if (isFirstSeedNode) cluster.selfAddress +: otherNodes
      else otherNodes
    cluster.joinSeedNodes(seedNodes)

    TestApp.start(system)
  }

  def isFirstSeedNode(): Boolean = {
    // In the OpsWorks chef recipes we can pass JVM arguments in the "Custom Chef JSON"
    // section to set this system property when launching the first seed node.
    // This is needed when starting the cluster the first time or when stopping all
    // nodes and starting them again (e.g. planned maintenance).
    // When restarting the first seed node it is not needed, because there is already
    // a running cluster to join, but it doesn't hurt to always set it.
    val first = System.getProperty("first-seed", "false").toLowerCase
    (first == "on" || first == "true")
  }

  /**
   * All started instances in the OpsWorks stack
   */
  def opsInstances(): Vector[Instance] = {
    try {
      val stackId = System.getProperty("ops-stack-id")
      require(stackId != null, "ops-stack-id must be defined")
      import scala.collection.JavaConverters._
      val client = new AWSOpsWorksClient
      val req = (new DescribeInstancesRequest).withStackId(stackId)
      val result = client.describeInstances(req)
      // PrivateIp is null when not started
      result.getInstances.asScala.collect { case i if i.getPrivateIp != null ⇒ i }(collection.breakOut)
    } catch {
      case e: Exception ⇒
        log.warn("OpsWorks not available, due to: {}", e.getMessage)
        throw e
    }
  }

  def shutdown: Unit = {
    if (system ne null) {
      system.shutdown()
      system = null
    }
  }

}

object OpsWorksMain {
  def main(args: Array[String]): Unit = {
    (new OpsWorksBoot).startup()
  }
}

