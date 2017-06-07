/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.typesafe.com>
 */
package com.lightbend.akka.bench.sharding

import akka.actor.{Actor, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberLeft
import com.lightbend.akka.bench.sharding.PersistenceHistograms.PrintHistograms
import org.HdrHistogram.Histogram

/**
 * Thread safe histograms updated from the persistent entity, an actor (should be a single one per actor system)
 * periodically printing the histograms to stdout
 */
object PersistenceHistograms {

  val persistTiming = new Histogram(20 * 1000, 3)
  val recoveryTiming = new Histogram(60 * 1000, 3)

  object PrintHistograms

  def props() = Props[PersistenceHistograms]
}

class PersistenceHistograms extends Actor {

  Cluster(context.system).subscribe(self, classOf[MemberLeft])

  override def receive: Receive = {
    case _: MemberLeft =>
      // as soon as members start leaving we can shutdown as well

      // not really useful yet
      // println("Histogram of persisted entity recoveries")
      // PersistenceHistograms.recoveryTiming.outputPercentileDistribution(System.out, 1.0)

      println("Histogram of persists")
      PersistenceHistograms.persistTiming.outputPercentileDistribution(System.out, 1.0)

      context.system.terminate()

  }
}
