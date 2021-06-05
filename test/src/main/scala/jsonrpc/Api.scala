package jsonrpc

import scala.concurrent.Future
import jsonrpc.spi.Backend

final case class SimpleApi():

  def method(test: String): String = test


trait Api[Effect[_]]:
  def method0(): Effect[Unit]

  def method1(): Effect[Double]

  def method2(p0: String): Effect[Unit]

  def method3(p0: Int): Effect[Seq[String]]

  def method4(p0: Option[Int]): Effect[Long]

  def method5(p0: String, p1: Int): Effect[Option[String]]

//  def method6(p0: Record)(using context: Short): Effect[Int]
//
//  def method7(p0: Record, p1: String)(using Short): Effect[Record]

  def method8(p0: Option[Boolean], p1: Float)(p2: List[Int]): Effect[Map[String, String]]

  def method9(p0: String): Effect[String]

  protected def protectedMethod: Unit


final case class ApiImpl[Effect[_]](backend: Backend[Effect]) extends Api[Effect]:

  /**
   * Test method.
   */
  def method0(): Effect[Unit] = backend.pure(())

  def method1(): Effect[Double] = backend.pure(1.2d)

  def method2(p0: String): Effect[Unit] = backend.pure(())

  def method3(p0: Int): Effect[Seq[String]] = backend.pure(Seq.fill(p0)("x"))

  def method4(p0: Option[Int]): Effect[Long] = backend.pure(p0.map(_ + 1).getOrElse(0))

  def method5(p0: String, p1: Int): Effect[Option[String]] = backend.pure(Some(s"$p0$p1"))

  def method6(p0: Record)(using context: Short): Effect[Int] = p0.int match
    case Some(int) => backend.pure(int + context)
    case _         => backend.pure(0)

  def method7(p0: Record, p1: String)(using Short): Effect[Record] =
    backend.pure(p0.copy(
      string = s"${p0.string} - $p1",
      long = p0.long + summon[Short],
      enumeration = Some(Enum.One)
    ))

  def method8(p0: Option[Boolean], p1: Float)(p2: List[Int]): Effect[Map[String, String]] =
    backend.pure(Map(
      "boolean" -> p0.getOrElse(false).toString,
      "float" -> p1.toString,
      "list" -> p2.mkString(", ")
    ))

  def method9(p0: String): Effect[String] = backend.failed(IllegalArgumentException(p0))

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
  float: Option[Float],
  double: Double,
  enumeration: Option[Enum],
  list: List[String],
  map: Map[String, Int],
  structure: Option[Structure],
  none: Option[String]
)

final case class Structure(
  value: String
)
