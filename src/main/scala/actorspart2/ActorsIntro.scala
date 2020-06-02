package actorspart2

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {

  // part1 - actor systems
  /**
    * Actor System is a heavyweight Data structure which controls number of threads under the hood.
    */
  val actorSystem = ActorSystem("firstActorSystem")

  // part2 - create actors
  class WordCountActor extends Actor {
    // internal data
    var totalWords = 0

    // behaviour
    def receive: Receive = { // Receive = PartialFunction[Any, Unit]
      case message: String =>
        totalWords += message.split(" ").length
        println("Word Count: " + totalWords)
      case msg => println(s"I cannot understand ${msg.toString}")
    }
  }

  // part3 - instantiate our actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")

  // part4 - communicating with an actor
  wordCounter ! "I'm learning Akka" // this sending of message is completely asynchronous.
  anotherWordCounter ! 3
  wordCounter ! "I'm learning Akka" // this will continue with the previous state

  // creating actor which have parameters
  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _ => println("oops!!!")
    }
  }

  object Person {
    // this is the best practice for creating actors with constructor arguments
    def props(name: String) = Props(new Person(name))
  }

  val person = actorSystem.actorOf(Person.props("Ayush"), "ayush")
  person ! "hi"

}
