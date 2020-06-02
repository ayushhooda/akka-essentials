package exercises

import akka.actor.{Actor, ActorSystem, Props}

object Ex1 extends App {

  /**
    * a Counter Actor
    * - Increment
    * - Decrement
    * - Print
    */

  val system = ActorSystem("ex-1")

  // DOMAIN of the counter
  object Counter {
    def props(count: Int): Props = Props(new Counter(count))
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter(var count: Int) extends Actor {
    import Counter._
    override def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => println("Count = " + count)
    }
  }

  val counter = system.actorOf(Counter.props(5), "counter-1")

  counter ! Counter.Increment
  counter ! Counter.Print



}
