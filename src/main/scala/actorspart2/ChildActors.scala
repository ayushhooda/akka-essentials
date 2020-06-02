package actorspart2

import actorspart2.ChildActors.CreditCard.CheckStatus
import actorspart2.ChildActors.NaiveBankAccount.{Deposit, InitializeAccount}
import actorspart2.ChildActors.Parent.{CreateChild, TellChild}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActors extends App {

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }

  class Parent extends Actor {
    import Parent._
    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child")
        // create a new actor right here
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) =>
        if(childRef != null) {
          childRef forward message
        }
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I get: $message")
    }
  }

  val system = ActorSystem("ParentChildDemo")

  val parent = system.actorOf(Props[Parent], "parent")
  parent ! CreateChild("child")
  parent ! TellChild("Hey Kid!")


  // Actors can create other actors using context.actorOf
  // This gives Akka the ability to create actor hierarchies
  // parent -> child -> grandChild
  //        -> child2

  /*
  Who is the parent of Parent Actor here?
  Guardian actors (top-level)
  - /system = system guardian
  - /user = user-level guardian (it manages all the actors created by user)
  - / =   the root guardian (it manages both the system level and user level guardian)
   */

  /**
    * Actor selection
    */
  val childSelection = system.actorSelection("/user/parent/child")
  childSelection ! "I found you"

  // Note: Use this when you want to relocate your actor deeper into hierarchy

  /**
    * DANGER:
    * NEVER PASS MUTABLE ACTOR STATE, OR THE 'THIS' REFERENCE TO CHILD ACTORS.
    * AS CHILD ACTOR CAN MUTATE PARENT ACTOR'S STATE AND ALSO HAS ACCESS TO IT'S METHODS.
    * IT BREAKS ENCAPSULATION.
    */

  // E.g.

  object NaiveBankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeAccount
  }

  class NaiveBankAccount extends Actor {
    import NaiveBankAccount._
    import CreditCard._
    var amount = 0
    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this)
      case Deposit(funds) => deposit(funds)
      case Withdraw(funds) => withdraw(funds)
    }

    def deposit(funds: Int) = {
      println(s"${self.path} depositing $funds on top of $amount")
      amount += funds
    }
    def withdraw(funds: Int) = {
      println(s"${self.path} withdrawing $funds from   $amount")
      amount -= funds
    }
  }

  object CreditCard {
    case class AttachToAccount(bankAccount: NaiveBankAccount)
    case object CheckStatus
  }

  class CreditCard extends Actor {
    import CreditCard._
    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attachedTo(account))
    }

    def attachedTo(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} your message has been processed.")
        // benign
        account.withdraw(1) // because i can and here is the problem
      // We never call methods directly from another actor, but communicate through messages
    }

  }

  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(100)

  Thread.sleep(500)
  val ccSelection = system.actorSelection("/user/account/card")
  ccSelection ! CheckStatus


}
