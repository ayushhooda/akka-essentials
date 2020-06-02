package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import akka.pattern.pipe

class AskSpec extends TestKit(ActorSystem("AskDemo"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._

  "An authenticator" should {
    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AuthFailure("Password not found"))
    }

    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "iloveakka")
      expectMsg(AuthFailure("password incorrect"))
    }

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AuthSuccess)
    }
  }

  "A Piped authenticator" should {
    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AuthFailure("Password not found"))
    }

    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "iloveakka")
      expectMsg(AuthFailure("password incorrect"))
    }

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AuthSuccess)
    }
  }

}

object AskSpec {

  // assume this code is somewhere else in your app

  case class Read(key: String)
  case class Write(key: String, value: String)
  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at the key $key")
        sender() ! kv.get(key) // Option[String]
      case Write(key: String, value: String) =>
        log.info(s"Writing the value $value for the key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  // user authenticator actor
  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(msg: String)
  case object AuthSuccess
  class AuthManager extends Actor with ActorLogging {

    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    protected val authDb = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username, password) =>
        authDb ! Write(username, password)
      case Authenticate(username, password) =>
        handleAuthentication(username, password)
    }

    def handleAuthentication(username: String, password: String): Unit = {
      val originalSender = sender()
      val future = authDb ? Read(username)
      future.onComplete {
        // NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACCESS MUTABLE STATE IN ONCOMPLETE.
        // avoid closing over the actor instance or mutable state
        case Success(None) => originalSender ! AuthFailure("Password not found")
        case Success(Some(dbPassword)) =>
          if (dbPassword == password) originalSender ! AuthSuccess
          else originalSender ! AuthFailure("password incorrect")
        case Failure(exception) => originalSender ! AuthFailure(exception.toString)
      }
    }
  }

  class PipedAuthManager extends AuthManager {
    override def handleAuthentication(username: String, password: String): Unit = {
      val future = authDb ? Read(username) // Future[Any]
      val passwordFuture = future.mapTo[Option[String]] // Future[Option[String]]
      val responseFuture = passwordFuture.map {
        case None => AuthFailure("Password not found")
        case Some(dbPassword) =>
          if (dbPassword == password) AuthSuccess
          else AuthFailure("password incorrect")
      } // Future[Any]
      /*
      when the Future completes, send the response to sender
       */
      responseFuture.pipeTo(sender()) // this will help in restricting the use of onComplete
    }
  }

}

/**
  * Note:- Use ask when you expect a single response
  */
