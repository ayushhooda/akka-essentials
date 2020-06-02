package part1recap

object AdvancedRecap extends App {

  // partial functions
  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 10
    case 2 => 20
  }

  val pf = (x: Int) => x match {
    case 1 => 10
    case 2 => 20
  }

  // lifting
  val lifted = partialFunction.lift // total function Int => Option[Int]
  lifted(2) // Some(20)

  // type aliases
  type ReceiveFunction = PartialFunction[Any, Unit]

  def receive: ReceiveFunction = {
    case 1 => print("Hello")
    case _ => print("Confused")
  }

  // implicits
  implicit val timeout: Int = 3000
  def setTimeout(f: Int => Int)(implicit timeout: Int): Int = f(timeout)
  print(setTimeout((x: Int) => x * x))

  // implicit conversions
  // 1. implicit defs
  case class Person(name: String) {
    def greet: String = s"Hi my name is $name"
  }

  implicit def fromStringToPerson(str: String): Person = Person(str)

  "Ayush".greet
  // fromStringToPerson("Ayush").greet

  // 2. implicit class
  implicit class Dog(name: String) {
    def bark: Unit = print("bark!")
  }

  "Pluto".bark

  // organize implicits
  // 1. local scope
  // 2. imported
  // 3. companion object






}
