package com.lightbend.re

import akka.actor.{ActorRef, ActorSystem, Props, ReceiveTimeout}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.PersistentActor
import NormalExample._
import akka.pattern.ask

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn

object ReplicatedEntityApp extends App {

  println("Lets do some sharding")
  val system = ActorSystem()
  import system.dispatcher

  val counterRegion: ActorRef = ClusterSharding(system).start(
    typeName = "Counter",
    entityProps = Props[Counter],
    settings = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId = extractShardId)

  val count: Future[Int] = (counterRegion ? Get(1)).mapTo[Int]

  count.onComplete(println)

  StdIn.readLine()
  system.terminate()
}

object NormalExample {
  case object Increment
  case object Decrement
  final case class Get(counterId: Long)
  final case class EntityEnvelope(id: Long, payload: Any)

  case object Stop
  final case class CounterChanged(delta: Int)

  class Counter extends PersistentActor {

    import akka.cluster.sharding.ShardRegion.Passivate

    context.setReceiveTimeout(120.seconds)

    // self.path.name is the entity identifier (utf-8 URL-encoded)
    override def persistenceId: String = "Counter-" + self.path.name

    var count = 0

    def updateState(event: CounterChanged): Unit =
      count += event.delta

    override def receiveRecover: Receive = {
      case evt: CounterChanged ⇒ updateState(evt)
    }

    override def receiveCommand: Receive = {
      case Increment ⇒ persist(CounterChanged(+1))(updateState)
      case Decrement ⇒ persist(CounterChanged(-1))(updateState)
      case Get(_) ⇒ sender() ! count
      case ReceiveTimeout ⇒ context.parent ! Passivate(stopMessage = Stop)
      case Stop ⇒ context.stop(self)
    }
  }

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case EntityEnvelope(id, payload) ⇒ (id.toString, payload)
    case msg@Get(id) ⇒ (id.toString, msg)
  }

  val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case EntityEnvelope(id, _) ⇒ (id % numberOfShards).toString
    case Get(id) ⇒ (id % numberOfShards).toString
    case ShardRegion.StartEntity(id) ⇒
      // StartEntity is used by remembering entities feature
      (id.toLong % numberOfShards).toString
  }
}
