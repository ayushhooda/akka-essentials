package exercises

import akka.actor.{Actor, ActorSystem, Props}

/**
  * Recreate Counter Actor without mutable state i.e., by using context.become
  */

object Ex3 extends App {

  val system = ActorSystem("ex-3")

  // DOMAIN of the counter
  object Counter {
    def props: Props = Props(new Counter)
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    import Counter._
    override def receive: Receive = countReceive(0)

    def countReceive(count: Int): Receive = {
      case Increment => context.become(countReceive(count + 1))
      case Decrement => context.become(countReceive(count - 1))
      case Print => println("Count: " + count)
    }

  }

  val counter = system.actorOf(Counter.props, "counter-1")

  counter ! Counter.Increment
  counter ! Counter.Print
  counter ! Counter.Decrement
  counter ! Counter.Decrement
  counter ! Counter.Decrement
  counter ! Counter.Increment
  counter ! Counter.Print

  /**
    * Note: We can avoid mutable state with the help of context.become and using parameters.
    */

}
