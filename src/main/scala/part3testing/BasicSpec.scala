package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
// Implicit Sender is passing testActor with every single message as sender
  with WordSpecLike
  with BeforeAndAfterAll {

  //setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import BasicSpec._
  "A Simple Actor" should {
    "send back the same message" in {
      val echoActor = system.actorOf(Props[SimpleActor])
      val msg = "Hello! Actor"
      echoActor ! msg

      expectMsg(msg) // akka.test.single-expect-default - configuration to change timeout
    }
  }

  "A BlackHole Actor" should {
    "don't send any message" in {
      val blackHole = system.actorOf(Props[BlackHole])
      val msg = "Hello! Actor"
      blackHole ! msg

      expectNoMessage(1 second)
    }
  }

  // message assertions
  "A lab test actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn a string into uppercase" in {
      labTestActor ! "I love Akka"
      val reply = expectMsgType[String] // hold the reply
      assert(reply == "I LOVE AKKA")
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("hi", "hello")
    }

    "reply with favourite tech" in {
      labTestActor ! "favouriteTech"
      expectMsgAllOf("Scala", "Akka")
    }

    "reply with favourite tech in a different way" in {
      labTestActor ! "favouriteTech"
      val messages = receiveN(2) // Seq[AnyRef]

      // free to do more complicated assertions
    }

    "reply with favourite tech in more different way" in {
      labTestActor ! "favouriteTech"

      // most powerful way of testing so far
      expectMsgPF() {
        case "Scala" => // only care that the PF is defined
        case "Akka" =>
      }
    }

  }


}

object BasicSpec {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case msg => sender() ! msg
    }
  }

  class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greeting" =>
        if (random.nextBoolean()) sender ! "hi" else sender ! "hello"
      case "favouriteTech" =>
        sender ! "Scala"
        sender ! "Akka"
      case msg: String => sender() ! msg.toUpperCase
    }
  }


}
