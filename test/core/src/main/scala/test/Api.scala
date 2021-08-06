package test

import automorph.spi.EffectSystem
import test.{Enum, Record}

trait SimpleApi[Effect[_]] {

  def test(test: String): Effect[String]
}

final case class SimpleApiImpl[Effect[_]](backend: EffectSystem[Effect]) extends SimpleApi[Effect] {

  override def test(test: String): Effect[String] = backend.pure(test)
}

trait ComplexApi[Effect[_], Context] {

  /** Test method. */
  def method0(): Effect[Unit]

  def method1(): Effect[Double]

  def method2(p0: String): Effect[Unit]

  def method3(p0: Float, p1: Long, p2: Option[List[Int]]): Effect[List[String]]

  def method4(p0: BigDecimal, p1: Byte, p2: Map[String, Int], p3: Option[String]): Effect[Long]

  def method5(p0: Boolean, p1: Short)(p2: List[Int]): Effect[Map[String, String]]

  def method6(p0: Record, p1: Double): Effect[Option[String]]

  def method7(p0: Record, p1: Boolean)(implicit context: Context): Effect[Int]

  def method8(p0: Record, p1: String, p2: Option[Double])(implicit context: Context): Effect[Record]

  def method9(p0: String): Effect[String]

  protected def protectedMethod(): Unit
}

final case class ComplexApiImpl[Effect[_], Context](backend: EffectSystem[Effect]) extends ComplexApi[Effect, Context] {

  override def method0(): Effect[Unit] = backend.pure(())

  override def method1(): Effect[Double] = backend.pure(1.2d)

  override def method2(p0: String): Effect[Unit] = backend.pure(())

  override def method3(p0: Float, p1: Long, p2: Option[List[Int]]): Effect[List[String]] =
    backend.pure(p2.getOrElse(List(0)).map(number => (p1 + p0 + number).toString))

  override def method4(p0: BigDecimal, p1: Byte, p2: Map[String, Int], p3: Option[String]): Effect[Long] =
    backend.pure(p0.sign.toLong + p1 + p2.values.sum + p3.map(_.length).getOrElse(0))

  override def method5(p0: Boolean, p1: Short)(p2: List[Int]): Effect[Map[String, String]] =
    backend.pure(Map(
      "boolean" -> p0.toString,
      "float" -> p1.toString,
      "list" -> p2.mkString(", ")
    ))

  override def method6(p0: Record, p1: Double): Effect[Option[String]] =
    backend.pure(Some((p0.double + p1).toString))

  override def method7(p0: Record, p1: Boolean)(implicit context: Context): Effect[Int] = p0.int match {
    case Some(int) if p1 => backend.pure(int + context.toString.length)
    case _ => backend.pure(0)
  }

  override def method8(p0: Record, p1: String, p2: Option[Double])(implicit context: Context): Effect[Record] =
    backend.pure(p0.copy(
      string = s"${p0.string} - $p1 - ${implicitly[Context].toString}",
      long = p0.long + p2.map(_.toLong).getOrElse(0L),
      double = p2.getOrElse(0.1),
      enumeration = Enum.fromOrdinal(1)
    ))

  override def method9(p0: String): Effect[String] = backend.failed(new IllegalArgumentException(p0))

  protected def protectedMethod(): Unit = privateMethod()

  private def privateMethod(): Unit = ()
}

trait InvalidApi[Effect[_]] {

  def nomethod(p0: String): Effect[Unit]

  def method1(p0: String): Effect[Unit]

  def method2(p0: String): Effect[String]

  def method3(p0: Float, p1: Option[Long]): Effect[List[String]]

  def method4(p0: BigDecimal, p1: String, p2: String): Effect[String]
}

final case class InvalidApiImpl[Effect[_]](backend: EffectSystem[Effect]) extends InvalidApi[Effect] {

  def nomethod(p0: String): Effect[Unit] = backend.pure(())

  def method1(p0: String): Effect[Unit] = backend.pure(())

  def method2(p0: String): Effect[String] = backend.pure("")

  def method3(p0: Float, p1: Option[Long]): Effect[List[String]] = backend.pure(List())

  def method4(p0: BigDecimal, p1: String, p2: String): Effect[String] = backend.pure("")
}
