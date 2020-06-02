package part4faulttolerance

import java.io.File

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{Backoff, BackoffSupervisor}

import scala.concurrent.duration._
import scala.io.Source

object BackOffSupervisorPattern extends App {

  case object ReadFile
  class FileBasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Source = null

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.warning("Persistent Actor restarting")

    override def preStart(): Unit =
      log.info("Persistent actor starting")

    override def postStop(): Unit =
      log.info("Persistent actor has stopped")

    override def receive: Receive = {
      case ReadFile =>
        if (dataSource == null)
          dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
        log.info("I've read some IMPORTANT data: " + dataSource.getLines().toList)
    }
  }

  val system = ActorSystem("BackOffSupervisorDemo")
//  val simpleActor = system.actorOf(Props[FileBasedPersistentActor], "simpleActor")
//  simpleActor ! ReadFile
  /*
  Now suppose if I change important.txt to important_data.txt.
  Actor will throw FileNotFoundException and User Guardian will try to restart it.
  Suppose instead of file, we have a DB which goes down, and there are lot more actors trying to persist into that DB
  So this is a problem, all actors will be restarted again and again and hit DB continuously
   */

  val simpleSupervisorProps = BackoffSupervisor.props(
    Backoff.onFailure(
      Props[FileBasedPersistentActor],
      "simpleBackOffActor",
      3 seconds, // then 6s, 12s, 24s
      30 seconds, // max time cap
      0.2 // it adds a little bit of noise so that all actor doesn't restart at same time
    )
  )

//  val simpleBackOffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
//  simpleBackOffSupervisor ! ReadFile
  /*
  SimpleSupervisor
    - child called simpleBackOffActor (props of type FileBasedPersistentActor)
    (So basically two actors are formed here, simpleSupervisor is parent and it automatically forward msg to child)
    - supervision strategy is default(restarting on everything)
      - first attempt after 3s
      - next attempt is 2x the previous attempt
   */

  val stopSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[FileBasedPersistentActor],
      "stopBackOffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

//  val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
//  stopSupervisor ! ReadFile

  class EagerFBPActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info(s"Eager actor starting")
      dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
    }
  }

//  val eagerActor = system.actorOf(Props[EagerFBPActor])

  // ActorInitializationException => STOP

  val repeatedSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[EagerFBPActor],
      "eagerActor",
      1 second,
      30 seconds,
      0.1
    )
  )

  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")
  repeatedSupervisor ! ReadFile
  /*
  eagerSupervisor
    - child eagerActor
      - will die on start with ActorInitializationException
      - trigger the supervision strategy in eagerSupervisor => STOP eagerActor
    - backoff will kick in after 1s, 2s, 4s, 8s, 16s
    (Here, it will fail again and again as dataSource is read in preStart)
    (Note: While running this, if you rename the file in between to correct name, exception will go away)
   */

}

/**
  * Back-off supervisor pattern is used to solve:
  * Pain: The repeated restarts os actor
  * (Specially in the context of actor interacting with external resources like DB)
  * Restarting actors immediately might do more harm than good
  */