package com.lightbend.akka.bench.sharding.scalability

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import com.lightbend.akka.bench.sharding.BenchSettings

object ActorCountingEntity {

  sealed trait HasId { def id: String }
  // sent from master to entity
  final case class Start(id: Integer, sentTimestamp: Long) extends HasId
  // entity replies with that once it has started, the time is the sender's time, 
  // so the sender can calculate how long it took for the actor to get the message
  final case class Ready(sentTimestamp: Long)

  def props() = Props[ActorCountingEntity]

  // sharding config
  val typeName = "bench-entity"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg: ActorCountingEntity.HasId => (msg.id, msg)
  }

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case msg: ActorCountingEntity.HasId => (Math.abs(msg.id.hashCode) % numberOfShards).toString
  }

  def startRegion(system: ActorSystem) =
    ClusterSharding(system).start(
      typeName,
      ActorCountingEntity.props(),
      ClusterShardingSettings(system).withRole("shard"),
      extractEntityId,
      extractShardId(BenchSettings(system).NumberOfShards)
    )

  def proxy(system: ActorSystem): ActorRef =
    ClusterSharding(system)
      .startProxy(
        typeName,
        Some("shard"),
        extractEntityId,
        extractShardId(BenchSettings(system).NumberOfShards)
      )
}


/** Many many many instances of this Actor will be started in sharding. */
class ActorCountingEntity extends Actor {

  override def receive: Receive = {
    case ActorCountingEntity.Start(_, senderTimestamp) =>
      sender() ! ActorCountingEntity.Ready(senderTimestamp)
      // stay around in memory, forevermore!
  }
}
