package jsonrpc

import scala.concurrent.Future

trait Api:

  def method0(): Double

  def method1(p0: Record): Future[Int]

  def method2(p0: Record, p1: String): Record

  def method3(p0: Option[Boolean], p1: Float)(p2: List[Int]): Map[String, String]

  def method4(): Future[Unit]

  def method5(p0: Option[String]): Unit

  def method6(p0: String): Unit

  protected def protectedMethod: Unit


final case class ApiImpl(test: String) extends Api:

  /**
   * Test method.
   *
   * @return test
   */
  def method0(): Double = 1.2d

  def method1(p0: Record): Future[Int] = p0.int match
    case Some(int) => Future.successful(int + 1)
    case _         => Future.successful(0)

  def method2(p0: Record, p1: String): Record =
    p0.copy(
      string = s"${p0.string} - $p1",
      long = p0.long + 1,
      enumeration = Enum.One
    )

  def method3(p0: Option[Boolean], p1: Float)(p2: List[Int]): Map[String, String] =
    Map(
      "boolean" -> p0.getOrElse(false).toString,
      "float"   -> p1.toString,
      "list"    -> p2.mkString(", ")
    )

  def method4(): Future[Unit] = Future.unit

  def method5(p0: Option[String]): Unit = ()

  def method6(p0: String): Unit = throw new IllegalArgumentException(p0)

  protected def protectedMethod = ()

  private def privateMethod = ()

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
