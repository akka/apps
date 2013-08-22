package testapp

import scala.annotation.tailrec
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.FromConfig
import akka.actor.ReceiveTimeout
import scala.concurrent.duration._

class FactorialFrontend(n: Int, batchSize: Int) extends Actor with ActorLogging {

  val backend = context.actorOf(Props.empty.withRouter(FromConfig),
    name = "factorialBackendRouter")
  var c = 0
  var startTime = 0L
  var durations = Vector.empty[Long]
  context.setReceiveTimeout(1.minute)

  override def preStart(): Unit = sendJobs()

  def receive = {
    case factorial: String ⇒
      c += 1
      if (c == batchSize) {
        val d = (System.nanoTime - startTime) / 1000
        durations :+= d
        if (durations.size == 10) {
          log.info("{} x {}! took [{}] us", batchSize, n, durations.mkString(", "))
          durations = Vector.empty[Long]
        }
        c = 0
        sendJobs()
      }
    case ReceiveTimeout ⇒
      log.info("Timeout, starting again")
      c = 0
      sendJobs()
  }

  def sendJobs(): Unit = {
    startTime = System.nanoTime
    1 to batchSize foreach { _ ⇒ backend ! n }
  }
}

class FactorialBackend extends Actor with ActorLogging {

  val workers = context.actorOf(Props[FactorialWorker].withRouter(FromConfig),
    name = "workers")

  def receive = {
    case n: Int ⇒ workers forward n
  }

}

class FactorialWorker extends Actor with ActorLogging {

  def receive = {
    case n: Int ⇒ sender ! factorial(n).toString
  }

  def factorial(n: Int): BigInt = {
    @tailrec def factorialAcc(acc: BigInt, n: Int): BigInt = {
      if (n <= 1) acc
      else factorialAcc(acc * n, n - 1)
    }
    factorialAcc(BigInt(1), n)
  }

}
