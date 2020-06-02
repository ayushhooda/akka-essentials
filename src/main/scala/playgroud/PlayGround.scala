package playgroud

import akka.actor.ActorSystem

object PlayGround extends App {

  val actorSystem = ActorSystem("HelloAkka")
  print(actorSystem.name)

}
