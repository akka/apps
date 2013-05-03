package testapp

import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator._
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.FromConfig

case class StatsJob(text: String)
case class StatsResult(meanWordLength: Double)
case class JobFailed(reason: String)

class StatsService extends Actor {
  val workerRouter = context.actorOf(Props[StatsWorker].withRouter(FromConfig),
    name = "workerRouter")

  DistributedPubSubExtension(context.system).mediator ! Put(self)

  def receive = {
    case StatsJob(text) if text != "" ⇒
      val words = text.split(" ")
      val replyTo = sender // important to not close over sender
      // create actor that collects replies from workers
      val aggregator = context.actorOf(Props(
        classOf[StatsAggregator], words.size, replyTo))
      words foreach { word ⇒
        workerRouter.tell(
          ConsistentHashableEnvelope(word, word), aggregator)
      }
  }
}

class StatsAggregator(expectedResults: Int, replyTo: ActorRef) extends Actor {
  var results = IndexedSeq.empty[Int]
  context.setReceiveTimeout(5.seconds)

  def receive = {
    case wordCount: Int ⇒
      results = results :+ wordCount
      if (results.size == expectedResults) {
        val meanWordLength = results.sum.toDouble / results.size
        replyTo ! StatsResult(meanWordLength)
        context.stop(self)
      }
    case ReceiveTimeout ⇒
      replyTo ! JobFailed("Service unavailable, try again later")
      context.stop(self)
  }
}

class StatsWorker extends Actor {
  var cache = Map.empty[String, Int]
  def receive = {
    case word: String ⇒
      val length = cache.get(word) match {
        case Some(x) ⇒ x
        case None ⇒
          val x = word.length
          cache += (word -> x)
          x
      }

      sender ! length
  }
}

class StatsClient extends Actor with ActorLogging {
  val cluster = Cluster(context.system)
  val mediator = DistributedPubSubExtension(context.system).mediator

  import context.dispatcher
  val tickInterval = Duration(context.system.settings.config.getMilliseconds("stats.tick-interval"), MILLISECONDS)
  val tickTask = context.system.scheduler.schedule(tickInterval, tickInterval, self, "tick")

  override def postStop(): Unit = {
    tickTask.cancel()
  }

  var okCount = 0
  var failCount = 0

  def receive = {
    case "tick" ⇒
      mediator ! SendToAll("/user/stats/singleton", StatsJob("this is the text that will be analyzed"))
    case result: StatsResult ⇒
      if (okCount == 0)
        log.info("Stats result [{}], after [{}] failed attempts", result, failCount)
      okCount += 1
      failCount = 0
    case failed: JobFailed ⇒
      if (failCount == 0)
        log.info("Stats failed [{}], after [{}] sucessful requests", failed, okCount)
      failCount += 1
      okCount = 0
  }

}
