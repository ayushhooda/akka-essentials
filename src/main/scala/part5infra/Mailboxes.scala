package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App {

  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case msg =>
        log.info(msg.toString)
    }
  }

  /**
    * Interesting case #1 - custom priority mailbox
    * P0 - most important
    * P1
    * P2
    * P3
    */

  // step 1 - mailbox definition
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(
      PriorityGenerator {
        case msg: String if msg.startsWith("[P0]") => 0
        case msg: String if msg.startsWith("[P1]") => 1
        case msg: String if msg.startsWith("[P2]") => 2
        case msg: String if msg.startsWith("[P3]") => 3
        case _ => 4
      })

  // step 2 - make it known in the config (application.conf)

  // step 3 - attach the dispatcher to an actor
  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
  supportTicketLogger ! PoisonPill // even this will be postponed
  supportTicketLogger ! "[P3] this thing would be nice to have"
  supportTicketLogger ! "[P0] this needs to be solved now"
  supportTicketLogger ! "[P1] do this when you have the time"
  // after which time i can send another msg and can be prioritized accordingly - no u can't

  /**
    * Interesting case #1 - control-aware mailbox
    * we'll use UnboundedControlAwareMailbox
    */
  // step 1 - mark important messages as control messages

  case object ManagementTicket extends ControlMessage

  /*
  step 2 - configure who gets the mailbox
    - make the actor attach to the mailbox
   */
  // method #1
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
  controlAwareActor ! "[P0] this needs to be solved now"
  controlAwareActor ! "[P1] do this when you have the time"
  controlAwareActor ! ManagementTicket

  // method #2 - using deployment config
  val altControlAwareActor = system.actorOf(Props[SimpleActor], "altControlAwareActor")
  altControlAwareActor ! "[P0] this needs to be solved now"
  altControlAwareActor ! "[P1] do this when you have the time"
  altControlAwareActor ! ManagementTicket

}
