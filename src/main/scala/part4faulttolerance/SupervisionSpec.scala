package part4faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}


class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import SupervisionSpec._

  "A supervisor" should {

    "resume it's child in case of a minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! "hhhhhhhh hhhhhhhhhhh hhhhhhhhhh hhhhhhhhhh hhhhhh" // length greater than 20
      // parent will resume child actor i.e., internal state will be maintained
      child ! Report // This should give 3
      expectMsg(3)
    }

    "restart it's child in case of an empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! "" // parent will restart the actor, i.e., state is not maintained
      child ! Report // This should give 0
      expectMsg(0)
    }

    "terminate it's child in case of a major error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)

      child ! "akka is nice"
      val terminatedMsg = expectMsgType[Terminated]
      assert(terminatedMsg.actor == child)
    }

    "escalate an error when it doesn't know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! 43

      // Here Parent will first terminate all it's child and then escalate it's failure to it's own parent

      val terminatedMsg = expectMsgType[Terminated]
      assert(terminatedMsg.actor == child)
    }

  }

  "A kinder supervisor" should {
    "not kill children in case it's restarted or escalated failures" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Akka is cool"
      child ! Report
      expectMsg(3)

      child ! 45 // Here child is not killed, but user guardian restart everything
      child ! Report
      expectMsg(0)
    }
  }

  "An All for one supervisor" should {
    "apply the all-for-one strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor], "allForOneSupervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      supervisor ! Props[FussyWordCounter]
      val secondChild = expectMsgType[ActorRef]

      secondChild ! "Testing Supervision"
      secondChild ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept {
        child ! ""
      }

      Thread.sleep(500)

      secondChild ! Report
      expectMsg(0)
    }
  }

}

object SupervisionSpec {


  class Supervisor extends Actor {

    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender() ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // empty
    }
  }

  class AllForOneSupervisor extends Supervisor {

    /**
      * This strategy works in such way that parent applies this strategy to all child regardless of child who sent the exception
      */

    override val supervisorStrategy: SupervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

  }

  case object Report

  class FussyWordCounter extends Actor {
    var words = 0

    override def receive: Receive = {
      case "" => throw new NullPointerException("sentence is empty")
      case sentence: String =>
        if (sentence.length > 20) throw new RuntimeException("sentence is too big")
        else if (!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("sentence must start with uppercase")
        else words += sentence.split(" ").length
      case Report => sender() ! words
      case _ => throw new Exception("can only receive strings")
    }
  }

}

/**
  * It's fine if actor crash
  * Parent must decide upon their children's failure
  * When an actor fails, it
  *   - suspends it's children
  *   - sends a(special) message to it's parent
  * Parent can decide to
  *   - resume the actor
  *   - restart the actor (default)
  *   - stop the actor
  *   - escalate and fail itself
  */