package testapp

import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator._
import akka.actor.ActorLogging
import akka.actor.Address
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.Duration
import akka.cluster.ClusterEvent.MemberRemoved

object ResultCollector {
  case class AddMemberTook(node: Address, duration: FiniteDuration, clusterSize: Int)
  case class MemberUpConfirmedByAll(node: Address, duration: FiniteDuration, clusterSize: Int)
  case class NoticedUnreachable(node: Address, from: Address, clusterSize: Int)
  case class NoticedReachable(node: Address, from: Address, unrechableDuration: Duration, clusterSize: Int)
  case class NoticedRemoved(memberRemoved: MemberRemoved, from: Address, clusterSize: Int)
  case object Ack
}

class ResultCollector extends Actor with ActorLogging {
  import ResultCollector._

  DistributedPubSubExtension(context.system).mediator ! Put(self)

  def receive = handle andThen ack

  def handle: Receive = {
    case AddMemberTook(node, duration, size) ⇒
      log.info("Adding node [{}] took [{}] ms in [{}] nodes cluster", node, duration.toMillis, size)
    case MemberUpConfirmedByAll(node, duration, size) ⇒
      log.info("All confirmed Up of node [{}] took [{}] ms in [{}] nodes cluster", node, duration.toMillis, size)
    case NoticedRemoved(evt, from, size) ⇒
      log.info("Node [{}] noticed [{}] in [{}] nodes cluster", from, evt, size)
    case NoticedUnreachable(node, from, size) ⇒
      log.info("Node [{}] noticed unreachable [{}] in [{}] nodes cluster", from, node, size)
    case NoticedReachable(node, from, duration, size) ⇒
      val durationStr = duration match {
        case f: FiniteDuration ⇒ f.toMillis.toString
        case _                 ⇒ "unknown"
      }
      log.info("Node [{}] noticed reachable [{}] again after [{}] ms in [{}] nodes cluster", from, node, durationStr, size)
  }

  def ack: Receive = {
    case _ ⇒ sender ! Ack
  }

}