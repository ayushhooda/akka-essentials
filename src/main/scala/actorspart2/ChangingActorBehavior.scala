package actorspart2

import actorspart2.ChangingActorBehavior.Mom.MomStart
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehavior extends App {

  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }

  class FussyKid extends Actor {
    import FussyKid._
    import Mom._

    // internal state of the kid
    var state = HAPPY
    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if(state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false) // change my receive handler to sadReceive
      case Food(CHOCOLATE) => context.unbecome()
      case Ask(_) => sender() ! KidAccept
    }

    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) => context.unbecome()
      case Ask(_) => sender() ! KidReject
    }
  }

  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String) // do you want to play?
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }

  class Mom extends Actor {
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our interaction
        kidRef ! Food(VEGETABLE)
        kidRef ! Ask("Do you want to play?")
      case KidAccept => println("Yay! my kid is happy")
      case KidReject => println("My kid is sad, but atleast he is healthy!")
    }
  }

  val system = ActorSystem("ChangingActorBehaviorDemo")
  val fussyKid = system.actorOf(Props[FussyKid], "fussyKid")
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid], "statelessFussyKid")
  val mom = system.actorOf(Props[Mom], "mom")
  mom ! MomStart(fussyKid)
  mom ! MomStart(statelessFussyKid)

  // context.become is basically a method to change handler for upcoming messages.

  /**
    * context.become(Receive, Boolean) - value false for boolean means:
    * as and when context.become is called, receive handler is being added to stack, i.e.,
    * stack.push(sadReceive) stack.push(happyReceive)
    * If you want to remove the elements from stack, we should use, context.unbecome
    */

  // Using context.unbecome
  // If messages are Food(VEG), Food(VEG) and Food(Chocolate).
  // Stack will be updated accordingly
  //

  // Imp: we use context.become and context.unbecome to remove actor's mutable state.
  // Akka always uses the latest handler on top of the stack.
  // if stack is empty, it calls receive.

}
