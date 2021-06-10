package jsonrpc

import scala.concurrent.Future
import jsonrpc.spi.Backend
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.*

trait SimpleApi[Effect[_]]:

  def test(test: String): Effect[String]

final case class SimpleApiImpl[Effect[_]](backend: Backend[Effect]) extends SimpleApi[Effect]:

  override def test(test: String): Effect[String] = backend.pure(test)

trait ComplexApi[Effect[_]]:

  /** Test method. */
  def method0(): Effect[Unit]

  def method1(): Effect[Double]

  def method2(p0: String): Effect[Unit]

  def method3(p0: Short, p1: Seq[Int]): Effect[Seq[String]]

  def method4(p0: Option[Long], p1: Option[Byte], p2: Option[String]): Effect[Long]

  def method5(p0: Record, p1: Double): Effect[Option[String]]

  def method6(p0: Record, p1: Boolean)(using context: Short): Effect[Int]

  def method7(p0: Record, p1: String, p2: Option[Double])(using Short): Effect[Record]

  def method8(p0: Option[Boolean], p1: Float)(p2: List[Int]): Effect[Map[String, String]]

  def method9(p0: String): Effect[String]

  protected def protectedMethod: Unit

final case class ComplexApiImpl[Effect[_]](backend: Backend[Effect]) extends ComplexApi[Effect]:

  override def method0(): Effect[Unit] = backend.pure(())

  override def method1(): Effect[Double] = backend.pure(1.2d)

  override def method2(p0: String): Effect[Unit] = backend.pure(())

  override def method3(p0: Short, p1: Seq[Int]): Effect[Seq[String]] =
    backend.pure(p1.map(_.toString) :+ p0.toString)

  override def method4(p0: Option[Long], p1: Option[Byte], p2: Option[String]): Effect[Long] =
    backend.pure(p0.map(_ + 1).getOrElse(0L) + p1.getOrElse(0.toByte) + p2.map(_.size).getOrElse(0))

  override def method5(p0: Record, p1: Double): Effect[Option[String]] =
    backend.pure(Some((p0.double + p1).toString))

  override def method6(p0: Record, p1: Boolean)(using context: Short): Effect[Int] = p0.int match
    case Some(int) if p1 => backend.pure(int + context)
    case _               => backend.pure(0)

  override def method7(p0: Record, p1: String, p2: Option[Double])(using Short): Effect[Record] =
    backend.pure(p0.copy(
      string = s"${p0.string} - $p1",
      long = p0.long + summon[Short],
      double = p2.getOrElse(0.1),
      enumeration = Enum.One
    ))

  override def method8(p0: Option[Boolean], p1: Float)(p2: List[Int]): Effect[Map[String, String]] =
    backend.pure(Map(
      "boolean" -> p0.getOrElse(false).toString,
      "float" -> p1.toString,
      "list" -> p2.mkString(", ")
    ))

  override def method9(p0: String): Effect[String] = backend.failed(IllegalArgumentException(p0))

  protected def protectedMethod = ()

  private def privateMethod = ()

enum Enum:

  case Zero
  case One

case object Enum:

  given Arbitrary[Enum] = Arbitrary {
    Gen.choose(0, Enum.values.size - 1).map(Enum.fromOrdinal)
  }

final case class Record(
  string: String,
  boolean: Boolean,
  byte: Byte,
  short: Short,
  int: Option[Int],
  long: Long,
  float: Option[Float],
  double: Double,
  enumeration: Enum,
  list: List[String],
  map: Map[String, Int],
  structure: Option[Structure],
  none: Option[String]
)

case object Record:

  given Arbitrary[Record] = Arbitrary {
    for
      string <- arbitrary[String]
      boolean <- arbitrary[Boolean]
      byte <- arbitrary[Byte]
      short <- arbitrary[Short]
      int <- arbitrary[Option[Int]]
      long <- arbitrary[Long]
      float <- arbitrary[Option[Float]]
      double <- arbitrary[Double]
      enumeration <- arbitrary[Enum]
      list <- arbitrary[List[String]]
      map <- arbitrary[Map[String, Int]]
      structure <- arbitrary[Option[Structure]]
      none <- arbitrary[Option[String]]
    yield Record(
      string,
      boolean,
      byte,
      short,
      int,
      long,
      float,
      double,
      enumeration,
      list,
      map,
      structure,
      none
    )
  }

final case class Structure(
  value: String
)

case object Structure:

  given Arbitrary[Structure] = Arbitrary {
    for
      value <- arbitrary[String]
    yield Structure(value)
  }
