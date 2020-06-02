package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {
    var count = 0
    override def receive: Receive = {
      case msg =>
        count += 1
        log.info(s"[$count] $msg")
    }
  }

  val system = ActorSystem("DispatcherDemo"/*ConfigFactory.load().getConfig("dispatcherDemo")*/)

  // #1 - in code
  val actors = for(i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")
  val r = new Random()
  for(i <- 1 to 1000) {
    actors(r.nextInt(10)) ! i
  }

  // #2 - from config
  val rtjvm = system.actorOf(Props[Counter], "rtjvm")

  /**
    * Dispatchers implement the ExecutionContext trait
    */
  class DBActor extends Actor with ActorLogging {
    // solution #1
    implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")

    override def receive: Receive = {
      case msg => Future {
        // wait on a message
        Thread.sleep(5000)
        log.info(s"Success: $msg")
      }
    }
  }

  val dbActor = system.actorOf(Props[DBActor])
  dbActor ! "The meaning of life is 42"



}
