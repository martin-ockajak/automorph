package jsonrpc

import scala.concurrent.Future

final case class Api():

  def method0(): Double = 1.2d

  def method1(a0: Record): Future[Int] = a0.int match
    case Some(int) => Future.successful(int + 1)
    case _         => Future.successful(0)

  def method2(a0: Record, a1: String): Record =
    a0.copy(
      string = s"${a0.string} - $a1",
      long = a0.long + 1,
      enumeration = Enum.One
    )

  def method3(a0: Option[Boolean], a1: Float, a2: List[Int]): Map[String, String] =
    Map(
      "boolean" -> a0.getOrElse(false).toString,
      "float"   -> a1.toString,
      "list"    -> a2.mkString(", ")
    )

  def method4(): Future[Unit] = Future.unit

  def method5(a0: Option[String]): Unit = ()

  def method6(a0: String): Unit = throw new IllegalArgumentException(a0)

end Api

enum Enum:
  case Zero
  case One


final case class Record(
  string: String,
  boolean: Boolean,
  byte: Byte,
  short: Short,
  int: Option[Int],
  long: Long,
  float: Float,
  double: Double,
  enumeration: Enum,
  list: List[Number],
  map: Map[String, String],
  none: Option[String],
  record: Option[Record]
)