package typesafe

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem

import akka.actor.ActorDSL._


class TestSomeActor extends FunSpec with BeforeAndAfterAll {

  implicit val system_server = ActorSystem("countTestSystem")

  describe("BasicHello") {
    it("should save name") {

      val someActor = system_server.actorOf(SomeActor())

      implicit val tmpActor = actor(new Act {
        become {
          case count: Long => {
            println(s"current count is $count")
          }
        }
      })

      for (x <- 1 to 100) {
        someActor ! SayHello("Cordoba")
      }
      someActor ! GetCount

    }
  }

}
