package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifeCycle extends App {

  case object StartChild
  class LifecycleActor extends Actor with ActorLogging {

    override def preStart(): Unit = {
      log.info(s"I am starting")
    }

    override def postStop(): Unit = {
      log.info(s"I have stopped")
    }

    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifecycleActor], "child")
    }
  }

  val system = ActorSystem("LifecycleDemo")
  val parent = system.actorOf(Props[LifecycleActor], "parent")
  parent ! StartChild
  parent ! PoisonPill

  /**
    * restart
    */
  case object Fail
  case object FailChild
  case object CheckChild
  case object Check
  class Parent extends Actor with ActorLogging {
    private val child = context.actorOf(Props[Child], "supervisedChild")
    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChild => child ! Check
    }
  }
  class Child extends Actor with ActorLogging {
    override def preStart(): Unit = log.info(s"supervised child started")

    override def postStop(): Unit = log.info(s"supervised child stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info(s"supervised actor restarting because of ${reason.getMessage}")
    }

    override def postRestart(reason: Throwable): Unit = {
      log.info("supervised actor restarted")
    }

    override def receive: Receive = {
      case Fail =>
        log.warning("child will fail now")
        throw new RuntimeException("I failed")
      case Check =>
        log.info("alive and kicking")
    }
  }

  val supervisor = system.actorOf(Props[Parent], "supervisor")
  supervisor ! FailChild
  supervisor ! CheckChild

}

/**
  * Actor Instance -
  *   a. has methods
  *   b. may have internal state
  *
  * Actor reference -
  *   a. created with actorOf
  *   b. has mailbox and can receive messages
  *   c. contains one actor instance
  *   d. contains a uuid
  *
  * Actor Path -
  *   a. may or may not have actor ref inside
  *
  * Actor LifeCycle -
  *   a. Started - create a new ActorRef with a UUID at a given path
  *   b. Suspended - the actor ref will enqueue but NOT process more messages
  *   c. resumed - the actor ref will continue processing more messages
  *   d. restarted -
  *     i. suspend
  *     j. swap actor instance (old instance calls preRestart, replace actor instance, new instance calls postRestart)
  *     k. resume
  *     Note: Internal State is destroyed on restart
  *   e. stopped - frees actor ref within a path
  *     i. calls postStop
  *     j. All watching actors receive Terminated(ref)
  *     k. after stopping, another actor may be created at same path with different UUID, so a different actor ref.
**/