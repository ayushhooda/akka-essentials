package actorspart2

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    // context.self === self
    // context.sender === sender
    override def receive: Receive = {
      case "Hi!" =>
        print("=======" + sender)
        sender() ! "Hey, there" // replying to a message
      case message: String => println(s"[$self] I have received $message")
      case number: Int => println(s"[simple actor] I have received $number")
      case SpecialMessage(contents) => println(s"[simple actor] I have received $contents")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) =>
        print("-----------" + self)
        ref ! "Hi!"
      case WirelessPhoneMessage(content, ref) => ref forward content
    }
  }

  val actorSystem = ActorSystem("actorCapabilitiesDemo")

  val simpleActor = actorSystem.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello, actor"

  // who is the sender here = ?? null
  simpleActor ! 42

  // 1. messages can be of any type
  // a. messages must be immutable
  // b. messages must be serializable
  // in practice use case classes and case objects

  case class SpecialMessage(specialContent: String)
  simpleActor ! SpecialMessage("some special content")

  // 2. actors have information about their context and about themselves.
  // context.self === `this` in OOP

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("myself")

  // 3. actors can reply to messages
  val alice = actorSystem.actorOf(Props[SimpleActor], "alice")
  val bob = actorSystem.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  // Imp: alice receives this SayToHi message.
  alice ! SayHiTo(bob)

  // 4. Deadletters - Fake Actor used by Akka for handling messages that are not sent to anyone
  simpleActor ! "Hi!" // reply to me

  // 5. Forwarding messages
  // D -> A -> B
  // forwarding = sending the message with the original sender

  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi", bob) // no sender, alice is receiver



}
