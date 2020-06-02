package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorsExcercise extends App {

  // Distributed Word Counting

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }

  class WordCounterMaster extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case Initialize(nChildren) =>
        println(s"[master] initializing...")
        val childRefs = for (i <- 1 to nChildren) yield context.actorOf(Props[WordCounterWorker], s"wcw_$i")
        context.become(withChildren(childRefs, 0, 0, Map()))
    }

    def withChildren(childRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[master] i have received: $text - I will send it to child $currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currentTaskId, text)
        val child = childRefs(currentChildIndex)
        child ! task
        val nextChildIndex = (currentChildIndex + 1) % childRefs.length
        val newTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childRefs, nextChildIndex, newTaskId, newRequestMap))
      case WordCountReply(id, count) =>
        println(s"[master] I have received a reply for task id $id with $count")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childRefs, currentChildIndex, currentTaskId, requestMap - id))
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"${self.path} I have received a task $id with $text")
        val count = text.split(" ").length
        sender() ! WordCountReply(id, count)
    }
  }

  class TestActor extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(3)
        val texts = List("I love Akka", "Scala is super dope", "yes", "me too")
        texts.foreach(text => master ! text)
      case count: Int =>
        println(s"[Test actor] I received a reply $count")
    }
  }

  val actorSystem = ActorSystem("Word-Counter")
  val testActor = actorSystem.actorOf(Props[TestActor], "testActor")
  testActor ! "go"

  /*
  Create WordCounterMaster (WCM)
  send Initialize(10) to WCM
  send "Akka is awesome" to WCM
  WCM will send a WordCountTask("...") to one of its children
  child replies with a WordCountReply(3) to WCM
  WCM replies with 3 to the sender
  requester -> WCM -> WCW
          r <- WCM <-
   */

  // round robin logic
  // 1,2,3,4,5 and 7 tasks
  // 1,2,3,4,5,1,2

}
