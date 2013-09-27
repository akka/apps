package testapp

import scala.concurrent.duration._
import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.MemberStatus
import akka.cluster.Member
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator._
import testapp.ResultCollector._

object MemberListener {
  case class IsUp(node: Address, itinerary: List[Address])
  case object NotUpYet
  case object AllConfirmedUp

}

class MemberListener extends Actor with ActorLogging {
  import MemberListener._

  import context.dispatcher
  val startTime = System.nanoTime()
  var selfUpReported = false
  val cluster = Cluster(context.system)
  var unreachableTimestamps = Map.empty[Address, Long]

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent])
    cluster.subscribe(self, classOf[ReachabilityEvent])
  }

  override def postStop(): Unit =
    cluster unsubscribe self

  var nodes = Set.empty[Address]

  def receive = {
    case state: CurrentClusterState ⇒
      nodes = state.members.map(_.address)
      if (state.members.nonEmpty) {
        log.info("Current members: [{}]", state.members.mkString(", "))
        if (state.members.exists(m ⇒ m.address == cluster.selfAddress && m.status == MemberStatus.Up))
          reportSelfUp()
      }
    case MemberUp(member) ⇒
      nodes += member.address
      log.info("Member is Up: [{}] in [{}] nodes cluster", member.address, nodes.size)
      if (member.address == cluster.selfAddress)
        reportSelfUp()

    case UnreachableMember(member) ⇒
      unreachableTimestamps = unreachableTimestamps.updated(member.address, System.nanoTime())
      log.info("Member detected as unreachable: [{}] in [{}] nodes cluster", member.address, nodes.size)
      if (unreachableTimestamps.size < 10)
        report(NoticedUnreachable(member.address, cluster.selfAddress, nodes.size))
    case ReachableMember(member) ⇒
      val duration = unreachableTimestamps.get(member.address) match {
        case Some(t) ⇒ (System.nanoTime() - t).nanos
        case None    ⇒ Duration.Undefined
      }
      unreachableTimestamps = unreachableTimestamps - member.address
      log.info("Member detected as reachable again: [{}] in [{}] nodes cluster", member.address, nodes.size)
      report(NoticedReachable(member.address, cluster.selfAddress, duration, nodes.size))
    case evt @ MemberRemoved(member, _) ⇒
      nodes -= member.address
      log.info("Member is Removed: [{}] in [{}] nodes cluster", member.address, nodes.size)

      report(NoticedRemoved(evt, cluster.selfAddress, nodes.size))
      unreachableTimestamps = unreachableTimestamps - member.address
    case LeaderChanged(leader) ⇒
    case _: ClusterDomainEvent ⇒ // ignore

    case IsUp(node, itinerary) ⇒
      if (nodes contains node) {
        if (itinerary.isEmpty)
          sender ! AllConfirmedUp
        else {
          val next = context.actorSelection(RootActorPath(itinerary.head) / self.path.elements)
          next.tell(IsUp(node, itinerary.tail), sender)
        }
      } else {
        sender ! NotUpYet
      }

  }

  def report(message: Any): Unit =
    context.actorOf(Props(classOf[ReportUntilAck], message))

  def reportSelfUp(): Unit =
    if (!selfUpReported) {
      selfUpReported = true
      val duration = (System.nanoTime() - startTime).nanos
      val clusterSize = nodes.size
      report(AddMemberTook(cluster.selfAddress, duration, clusterSize))
      if (clusterSize == 1)
        report(MemberUpConfirmedByAll(cluster.selfAddress, duration, clusterSize))
      else
        context.actorOf(Props(classOf[ReportWhenAllConfirmedUp], startTime, nodes))
    }

}

class ReportUntilAck(message: Any) extends Actor {
  context.setReceiveTimeout(3.seconds)
  val mediator = DistributedPubSubExtension(context.system).mediator
  val resultCollectorPath = "/user/resultsCollector/singleton"

  override def preStart(): Unit = {
    mediator ! SendToAll(resultCollectorPath, message)
  }

  def receive = {
    case Ack            ⇒ context.stop(self)
    case ReceiveTimeout ⇒ mediator ! SendToAll(resultCollectorPath, message)
  }
}

object ReportWhenAllConfirmedUp {
  object Start
}

class ReportWhenAllConfirmedUp(startTime: Long, nodes: Set[Address]) extends Actor with ActorLogging {
  import MemberListener._
  import context.dispatcher
  context.setReceiveTimeout(2.minutes)

  val selfAddress = Cluster(context.system).selfAddress
  val itinerary: List[Address] = {
    require(nodes contains selfAddress)
    import Member.addressOrdering
    val sorted = (Vector.empty[Address] ++ nodes).sorted
    val i = sorted.indexOf(selfAddress)
    val (a, b) = sorted.splitAt(i)
    (b.tail ++ a).toList
  }

  self ! ReportWhenAllConfirmedUp.Start

  def receive = {
    case ReportWhenAllConfirmedUp.Start ⇒
      val next = context.actorSelection(RootActorPath(itinerary.head) / context.parent.path.elements)
      next.tell(IsUp(selfAddress, itinerary.tail), sender)
    case NotUpYet ⇒
      context.system.scheduler.scheduleOnce(1.second, self, ReportWhenAllConfirmedUp.Start)
    case AllConfirmedUp ⇒
      context.setReceiveTimeout(Duration.Undefined)
      val duration = (System.nanoTime() - startTime).nanos
      val msg = MemberUpConfirmedByAll(selfAddress, duration, nodes.size)
      val reporter = context.actorOf(Props(classOf[ReportUntilAck], msg))
      context.watch(reporter)
      context.become {
        case Terminated(`reporter`) ⇒ context.stop(self)
      }
    case ReceiveTimeout ⇒
      log.error("Failed to report when all confirmed up [{}] - [{}]", selfAddress, itinerary.mkString(","))
      context.stop(self)
  }

}