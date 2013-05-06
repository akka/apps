package testapp

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

class MemberListener extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent])
    cluster.subscribe(self, classOf[UnreachableMember])
  }

  override def postStop(): Unit =
    cluster unsubscribe self

  var nodes = Set.empty[Address]

  def receive = {
    case state: CurrentClusterState ⇒
      nodes = state.members.map(_.address)
      log.info("Current members: [{}]", state.members.mkString(", "))
    case MemberUp(member) ⇒
      nodes += member.address
      log.info("Member is Up: [{}] in [{}] nodes cluster", member.address, nodes.size)
    case UnreachableMember(member) ⇒
      log.info("Member detected as unreachable: [{}] in [{}] nodes cluster", member.address, nodes.size)
    case MemberRemoved(member) ⇒
      nodes -= member.address
      log.info("Member is Removed: [{}] in [{}] nodes cluster", member.address, nodes.size)
    case _: ClusterDomainEvent ⇒ // ignore
  }
}