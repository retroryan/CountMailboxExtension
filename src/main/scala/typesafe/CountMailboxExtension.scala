package typesafe

import akka.actor._
import java.util.concurrent.atomic.AtomicLong
import akka.dispatch._
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}
import com.typesafe.config.Config
import java.util.Queue
import scala.Some

object CountMailboxExtension
  extends ExtensionId[CountMailboxExtension]
  with ExtensionIdProvider {

  //The lookup method is required by ExtensionIdProvider,
  // so we return ourselves here, this allows us
  // to configure our extension to be loaded when
  // the ActorSystem starts up
  override def lookup = CountMailboxExtension

  //This method will be called by Akka
  // to instantiate our Extension
  override def createExtension(system: ExtendedActorSystem) = {
    println("creating CountMailboxExtension ")
    new CountMailboxExtension
  }

}

class CountMailboxExtension extends Extension {

  private val messageQueues = new ConcurrentHashMap[ActorRef, CountingUnboundedMailbox.MessageQueue]

  def register(actorRef: ActorRef, messageQueue: CountingUnboundedMailbox.MessageQueue): Unit = {
    //println(s"actorRef: $actorRef queue: $messageQueue")
    messageQueues.put(actorRef, messageQueue)
  }

  def unregister(actorRef: ActorRef): Unit = messageQueues.remove(actorRef)

  def getCount()(implicit context: ActorContext) = {

    //println(s"getting count of context $context.self")

    messageQueues.get(context.self) match {
      case null ⇒ throw new IllegalArgumentException("Mailbox not registered for: " + context.self)
      case messageQueue ⇒ messageQueue.getCount
    }
  }
}

/**
 * UnboundedMailbox is the default unbounded MailboxType used by Akka Actors.
 */
case class CountingUnboundedMailbox() extends MailboxType with ProducesMessageQueue[UnboundedMailbox.MessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = this()

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = {
    owner.foreach(ref => s"creating mailbox for $ref")

    (owner, system) match {
      case (Some(theOwner), Some(theActorSystem)) ⇒
        val queue = new CountingUnboundedMailbox.MessageQueue
        CountMailboxExtension(theActorSystem).register(theOwner, queue)
        queue
      case _ ⇒ throw new Exception("no mailbox owner or system given")
    }

  }
}

object CountingUnboundedMailbox {

  class MessageQueue extends ConcurrentLinkedQueue[Envelope] with UnboundedQueueBasedMessageQueue {
    final def queue: Queue[Envelope] = this

    private val counter = new AtomicLong(0)

    def getCount = counter.get

    override def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
      counter.getAndIncrement
      println(s"enq counter = $counter")
      queue add handle
    }

    override def dequeue(): Envelope = {
      counter.getAndDecrement
      println(s"deq counter = $counter")
      queue.poll()
    }

  }

}

