package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Ex2 extends App {

  /**
    * a Bank Account as an Actor
    * - Deposit an amount
    * - Withdraw an amount
    * - Statement
    *
    * - Withdraw/Deposit will reply with Success/Failure
    *
    * interact with some other kind of actor
    */

  // DOMAIN of BankAccount
  object BankAccount {
    case class Deposit(_amount: Int)
    case class Withdraw(_amount: Int)
    case object Statement
    case class TransactionSuccess(message: String)
    case class TransactionFailure(message: String)
  }

  class BankAccount extends Actor {
    import BankAccount._
    var _funds = 0
    override def receive: Receive = {
      case Deposit(_amount) =>
        if (_amount < 0) {
          sender() ! TransactionFailure("Invalid deposit amount")
        } else {
          _funds += _amount
          sender() ! TransactionSuccess(s"Successfully deposited ${_amount}")
        }
      case Withdraw(_amount) =>
        if (_amount < 0) {
          sender() ! TransactionFailure(s"Invalid withdraw amount")
        } else if (_amount > _funds) {
          sender() ! TransactionFailure(s"Insufficient funds")
        } else {
          _funds -= _amount
          sender() ! TransactionSuccess(s"Successfully withdraw ${_amount}")
        }
      case Statement => sender() ! s"Your balance is ${_funds}"
    }
  }

  object Person {
    case class LiveTheLife(account: ActorRef)
  }

  class Person extends Actor {
    import Person._
    import BankAccount._
    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(1000)
        account ! Withdraw(2000)
        account ! Withdraw(500)
        account ! Statement
      case msg: String => println(msg)
      case TransactionSuccess(msg) => self ! msg
      case TransactionFailure(msg) => self ! msg
    }
  }

  val system = ActorSystem("ex-2")

  val person = system.actorOf(Props[Person], "ayush")
  val bankAccount = system.actorOf(Props[BankAccount], "icici")
  person ! Person.LiveTheLife(bankAccount)

}
