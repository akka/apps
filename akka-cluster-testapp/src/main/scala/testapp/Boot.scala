package testapp

import java.net.InetAddress
import scala.collection.immutable
import org.slf4j.LoggerFactory
import com.amazonaws.services.opsworks.AWSOpsWorksClient
import com.amazonaws.services.opsworks.model.DescribeInstancesRequest
import com.amazonaws.services.opsworks.model.Instance
import com.typesafe.config.ConfigFactory

import akka.actor._
import akka.kernel.Bootable

class Boot extends Bootable {
  val log = LoggerFactory.getLogger(getClass)
  var system: ActorSystem = _

  def startup(): Unit = {
    val stackId = System.getProperty("ops-stack-id")
    val selfHostName = InetAddress.getLocalHost.getHostName
    val conf =
      if (stackId eq null)
        ConfigFactory.load
      else {
        // running in EC2 with OpsWorks deployment
        val instances = opsInstances(stackId).sortBy(_.getHostname)
        val ips = instances.take(5).map { i ⇒
          if (i.getPrivateIp eq null) i.getHostname // not started, but should still be in the seed-nodes
          else i.getPrivateIp
        }
        instances.collectFirst { case i if (i.getPrivateIp ne null) && (i.getHostname == selfHostName) ⇒ i.getPrivateIp } match {
          case None ⇒
            throw new IllegalArgumentException(s"Couldn't find my own [${selfHostName}] private ip in list of instances [${instances}]")
          case Some(selfIp) ⇒
            val seedNodesStr = ips.map("akka.tcp://TestApp@" + _ + ":2552").mkString("\"", "\",\"", "\"")
            log.info(s"[${selfHostName}/${selfIp}] starting with OpsWorks seed-nodes=[${seedNodesStr}]")
            ConfigFactory.parseString(s"""
              akka.remote.netty.tcp.hostname="${selfIp}"
              akka.cluster.seed-nodes=[${seedNodesStr}]
              """).withFallback(ConfigFactory.load)
        }
      }

    system = ActorSystem("TestApp", conf)
    TestApp.start(system)

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

/**
 * Can be started with
 * -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.seed-nodes.1=akka.tcp://TestApp@10.192.14.250:2552
 * and then
 * -Dakka.remote.netty.tcp.port=0 -Dakka.cluster.seed-nodes.1=akka.tcp://TestApp@10.192.14.250:2552
 */
object Main {
  def main(args: Array[String]): Unit = {
    (new Boot).startup()
  }
}

