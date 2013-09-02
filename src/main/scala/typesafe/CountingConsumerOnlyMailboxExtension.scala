package typesafe

import akka.actor._
import java.util.concurrent.atomic.AtomicLong
import akka.dispatch._
import java.util.concurrent.ConcurrentHashMap
import com.typesafe.config.Config
import scala.Some
import scala.annotation.tailrec

object CountingConsumerOnlyMailboxExtension
  extends ExtensionId[CountingConsumerOnlyMailboxExtension]
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
    new CountingConsumerOnlyMailboxExtension
  }

}

class CountingConsumerOnlyMailboxExtension extends Extension {

  private val messageQueues = new ConcurrentHashMap[ActorRef, CountingNodeMessageQueue]

  def register(actorRef: ActorRef, messageQueue: CountingNodeMessageQueue): Unit = {
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

case class CountingSingleConsumerOnlyUnboundedMailbox() extends MailboxType with ProducesMessageQueue[NodeMessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = this()

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = {

    owner.foreach(ref => s"creating mailbox for $ref")

    (owner, system) match {
      case (Some(theOwner), Some(theActorSystem)) ⇒
        val queue = new CountingNodeMessageQueue
        CountingConsumerOnlyMailboxExtension(theActorSystem).register(theOwner, queue)
        queue
      case _ ⇒ throw new Exception("no mailbox owner or system given")
    }
  }
}

class CountingNodeMessageQueue extends AbstractNodeQueue[Envelope] with MessageQueue with UnboundedMessageQueueSemantics {

  private val counter = new AtomicLong(0)

  def getCount = counter.get

  final def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
    counter.getAndIncrement
    println(s"enq counter = $getCount")
    add(handle)
  }

  final def dequeue(): Envelope = {
    counter.getAndDecrement
    println(s"deq counter = $getCount")
    poll()
  }

  final def numberOfMessages: Int = count()

  final def hasMessages: Boolean = !isEmpty()

  @tailrec final def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
    val envelope = dequeue()
    if (envelope ne null) {
      deadLetters.enqueue(owner, envelope)
      cleanUp(owner, deadLetters)
    }
  }
}
