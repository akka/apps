package testapp

import java.util.concurrent.atomic.AtomicInteger
import com.typesafe.config.Config
import akka.actor.{ ActorContext, ActorRef, ActorSystem, ExtendedActorSystem }
import akka.dispatch.{ Envelope, MailboxType, MessageQueue, UnboundedMailbox, UnboundedQueueBasedMessageQueue }
import akka.event.Logging

/**
 * Logs the mailbox size when exceeding the configured limit. It logs at most once per second
 * when the messages are enqueued or dequeued.
 *
 * Configuration:
 * <pre>
 * akka.actor.default-mailbox {
 *   mailbox-type = testapp.LoggingMailboxType
 *   size-limit = 10
 * }
 * </pre>
 */
class LoggingMailboxType(settings: ActorSystem.Settings, config: Config) extends MailboxType {
  override def create(owner: Option[ActorRef], system: Option[ActorSystem]) = (owner, system) match {
    case (Some(o), Some(s)) ⇒
      val limit = config.getInt("size-limit")
      val mailbox = new LoggingMailbox(o, s, limit)
      mailbox
    case _ ⇒ throw new Exception("no mailbox owner or system given")
  }
}

class LoggingMailbox(owner: ActorRef, system: ActorSystem, sizeLimit: Int) extends UnboundedMailbox.MessageQueue {

  private val queueSize = new AtomicInteger
  lazy private val m = new Metrics
  class Metrics {
    val log = Logging(system, classOf[LoggingMailbox])
    val path = owner.path.toString
    @volatile var logTime: Long = 0L
  }

  override def dequeue(): Envelope = {
    val x = super.dequeue()
    if (x ne null) {
      val size = queueSize.decrementAndGet
      logSize(size)
    }
    x
  }

  override def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
    super.enqueue(receiver, handle)
    val size = queueSize.incrementAndGet
    logSize(size)
  }

  def logSize(size: Int): Unit =
    if (size >= sizeLimit) {
      val now = System.nanoTime
      if (now - m.logTime > 1000000000L) {
        m.logTime = now
        m.log.info("Mailbox [{}] size [{}]", m.path, size)
      }
    }

  override def numberOfMessages: Int = queueSize.get

  override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
    super.cleanUp(owner, deadLetters)
  }
}