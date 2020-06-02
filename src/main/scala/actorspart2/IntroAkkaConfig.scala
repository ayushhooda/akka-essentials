package actorspart2

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
    * 1 - Inline configuration
    */
  val configString =
    """
      | akka {
      |   loglevel = "DEBUG"
      | }
    """.stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config))
  val actor = system.actorOf(Props[SimpleLoggingActor])
  actor ! "A message to remember"

  /**
    * 2 - Config file
    */

  val defaultConfigFileSystem = ActorSystem("defaultConfigFileDemo") // it will automatically pick application.conf
  val defaultActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])
  defaultActor ! "Remember me"

  /**
    * 3 - separate config in the same file
    */
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem  = ActorSystem("SpecialConfigDemo", specialConfig)
  val specialConfigActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])
  specialConfigActor ! "Remember me! I'm special"

  /**
    * 4 - separate config in another file
    */
  val separateConfig = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(s"separate config log level: ${separateConfig.getString("akka.loglevel")}")

  /**
    * 5 - Different file formats
    * JSON, Properties
    */
  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"json config: ${jsonConfig.getString("aJsonProperty")}")
  println(s"json config: ${jsonConfig.getString("akka.loglevel")}")

  val propsConfig = ConfigFactory.load("props/propsConfiguration.properties")
  println(s"prop config: ${propsConfig.getString("akka.loglevel")}")

}
