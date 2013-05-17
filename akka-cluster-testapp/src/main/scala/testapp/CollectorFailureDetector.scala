/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package testapp

import akka.remote.FailureDetector
import scala.collection.immutable._
import com.typesafe.config.Config
import scala.concurrent.duration.{FiniteDuration, Duration}
import java.util.concurrent.TimeUnit._
import akka.remote.FailureDetector.Clock
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import akka.event.Logging

object CollectorFailureDetector {
  case class State(intervals: IndexedSeq[Long] = IndexedSeq.empty, lastTimestamp: Option[Long] = None)
}

class CollectorFailureDetector(
  val acceptablePause: Long,
  val slidingWindowSize: Int,
  val firstHeartbeat: Long,
  val tag: String)(implicit clock: Clock) extends FailureDetector {
  import CollectorFailureDetector._

  def this(config: Config) =
    this(acceptablePause = config.getMilliseconds("acceptable-heartbeat-pause"),
    slidingWindowSize = config.getInt("sliding-window-size"),
    firstHeartbeat = config.getMilliseconds("heartbeat-interval"),
    tag = config.getString("tag"))

  private val state = new AtomicReference[State](State())

  override def isAvailable: Boolean = isMonitoring && (state.get.lastTimestamp match {
    case None    => false
    case Some(t) => (clock() - t) < acceptablePause
  })

  override def isMonitoring: Boolean = state.get.intervals.nonEmpty

  override def heartbeat(): Unit = {
    val newState = enqueueInterval()
    if (newState.intervals.size == slidingWindowSize)
      println(logText(newState.intervals))
  }

  @tailrec
  private def enqueueInterval(): State = {
    val oldState = state.get
    val newState = oldState.lastTimestamp match {
      case None    => oldState.copy(intervals = oldState.intervals :+ firstHeartbeat, lastTimestamp = Some(clock()))
      case Some(t) =>
        val currentTime = clock()
        val interval = currentTime - t
        State(intervals = (interval +: oldState.intervals).take(slidingWindowSize), lastTimestamp = Some(currentTime))
    }
    if (state.compareAndSet(oldState, newState)) newState else enqueueInterval()
  }

  val header = s"Current and previous heartbeat intervals (ms) [$tag]: "

  private def logText(intervals: IndexedSeq[Long]): String = {
    val b = new StringBuilder
    b.append(header)

    // Exponentially sample sliding window of intervals
    var idx = 1
    while (idx <= slidingWindowSize) {
      b.append("[")
      b.append(idx - 1)
      b.append("]: ")
      b.append(intervals(idx - 1))
      b.append("ms ")
      idx <<= 1
    }

    b.toString()
  }
}
