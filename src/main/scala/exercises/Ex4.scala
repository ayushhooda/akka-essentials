package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import exercises.Ex4.Citizen.{Vote, VoteStatusReply, VoteStatusRequest}
import exercises.Ex4.VoteAggregator.AggregateVotes

/**
  * A simplified voting system
  */

object Ex4 extends App {

  val system = ActorSystem("ex-4")

  object Citizen {
    case class Vote(candidate: String)
    case object VoteStatusRequest
    case class VoteStatusReply(candidate: Option[String])
  }

  class Citizen extends Actor {
    import Citizen._
    override def receive: Receive = {
      case Vote(c) => context.become(voted(c))
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
    }

    def voted(candidate: String): Receive = {
      case VoteStatusRequest => sender() ! VoteStatusReply(Some(candidate))
    }
  }

  object VoteAggregator {
    case class AggregateVotes(citizens: Set[ActorRef])
  }

  class VoteAggregator extends Actor {

    override def receive: Receive = awaitingCommand

    def awaitingCommand: Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(_ ! VoteStatusRequest)
        context.become(awaitingStatuses(citizens, Map()))
    }

    def awaitingStatuses(stillWaiting: Set[ActorRef], currentStats: Map[String, Int]): Receive = {
      case VoteStatusReply(None) => sender() ! VoteStatusRequest
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStats.getOrElse(candidate, 0)
        val newStats = currentStats + (candidate -> (currentVotesOfCandidate + 1))
        if(newStillWaiting.isEmpty) {
          println(s"[aggregator] poll stats: $newStats")
        } else {
          // still needs computation
          context.become(awaitingStatuses(newStillWaiting, newStats))
        }
    }
  }

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])

  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

}
