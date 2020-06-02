package actorspart2

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App {

  // #1 - explicit logging
  class SimpleActorWithExplicitLogger extends Actor {
    val logger = Logging(context.system, this)
    override def receive: Receive = {
      /*
      1 - DEBUG
      2 - INFO
      3 - WARN
      4 - ERROR
       */
      case message => logger.info(message.toString)
    }
  }

  val system = ActorSystem("LoggingDemo")
  val simpleActor = system.actorOf(Props[SimpleActorWithExplicitLogger])
  simpleActor ! "Logging a simple message"

  // #2- ActorLogging
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Two things: {} and {}", a, b)
      case message => log.info(message.toString)
    }
  }
  val actorWithLogging = system.actorOf(Props[ActorWithLogging])
  actorWithLogging ! "Logging a simple message by extending a trait"
  actorWithLogging ! (42, 65)

  // Logging is done asynchronously i.e., it is done by actors itself
  // You can insert other loggers like slf4j


}
