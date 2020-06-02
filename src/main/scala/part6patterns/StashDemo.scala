package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  /*
  ResourceActor
    - open => it can receive read/write requests to the resource
    - otherwise it will postpone all the read/write requests until the state is open

    ResourceActor is closed
      - Open => Switch to the open state
      - Read, Write messages are POSTPONED

    ResourceActor is open
      - Read, Write are handled
      - Close => Switch to close state

    [Open, Read, Read, Write]
    - switch to open state
    - read the data
    - read the data again
    - write the data

    [Read, Open, Write]
    - stash Read
      Stash: [Read]
    - Open => switch to open state
      Mailbox: [Read, Write]
    - read and write are handled
   */
  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  // step #1 - mix in the stash trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        // step #3 - unstashAll when you switch the message handler
        unstashAll()
        context.become(open)
      case msg =>
        log.info(s"Stashing $msg because I am closed")
        // step #2 - stash away what you can't handle
        stash()
    }

    def open: Receive = {
      case Close =>
        log.info(s"Closing resource")
        unstashAll()
        context.become(closed)
      case Read =>
        log.info(s"I've read the $innerData")
      case Write(data) =>
        log.info(s"I'm writing $data")
        innerData = data
      case msg =>
        log.info(s"Stashing $msg because I am open")
        // step #2 - stash away what you can't handle
        stash()
    }
  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])
  resourceActor ! Read
  resourceActor ! Open
  resourceActor ! Open
  resourceActor ! Write("I love stash")
  resourceActor ! Close
  resourceActor ! Read

}
