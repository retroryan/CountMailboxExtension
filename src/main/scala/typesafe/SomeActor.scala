package typesafe

import akka.actor.{Props, ActorLogging, Actor}

class SomeActor extends Actor with ActorLogging {

  def receive = {
    case SayHello(name: String) => sender ! ReturnGoodbye(name)
    case GetCount => sender ! CountingConsumerOnlyMailboxExtension(context.system).getCount
  }

}

object SomeActor {
  def apply(): Props = {
    val props: Props = Props(new SomeActor)
    println(s"mailbox = ${props.mailbox}")
    props
  }
}


case class SayHello(name: String)

case class ReturnGoodbye(name: String)

case object GetCount