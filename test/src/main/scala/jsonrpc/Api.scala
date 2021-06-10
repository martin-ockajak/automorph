package jsonrpc

import jsonrpc.spi.Backend

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

  def method4(p0: Long, p1: Byte, p2: Map[String, Int], p3: Option[String]): Effect[Long]

  def method5(p0: Boolean, p1: Float)(p2: List[Int]): Effect[Map[String, String]]

  def method6(p0: Record, p1: Double): Effect[Option[String]]

  def method7(p0: Record, p1: Boolean)(using context: Short): Effect[Int]

  def method8(p0: Record, p1: String, p2: Option[Double])(using Short): Effect[Record]

  def method9(p0: String): Effect[String]

  protected def protectedMethod: Unit

final case class ComplexApiImpl[Effect[_]](backend: Backend[Effect]) extends ComplexApi[Effect]:

  override def method0(): Effect[Unit] = backend.pure(())

  override def method1(): Effect[Double] = backend.pure(1.2d)

  override def method2(p0: String): Effect[Unit] = backend.pure(())

  override def method3(p0: Short, p1: Seq[Int]): Effect[Seq[String]] =
    backend.pure(p1.map(_.toString) :+ p0.toString)

  override def method4(p0: Long, p1: Byte, p2: Map[String, Int], p3: Option[String]): Effect[Long] =
    backend.pure(p0 + p1 + p2.values.sum + p3.map(_.size).getOrElse(0))

  override def method5(p0: Boolean, p1: Float)(p2: List[Int]): Effect[Map[String, String]] =
    backend.pure(Map(
      "boolean" -> p0.toString,
      "float" -> p1.toString,
      "list" -> p2.mkString(", ")
    ))

  override def method6(p0: Record, p1: Double): Effect[Option[String]] =
    backend.pure(Some((p0.double + p1).toString))

  override def method7(p0: Record, p1: Boolean)(using context: Short): Effect[Int] = p0.int match
    case Some(int) if p1 => backend.pure(int + context)
    case _               => backend.pure(0)

  override def method8(p0: Record, p1: String, p2: Option[Double])(using Short): Effect[Record] =
    backend.pure(p0.copy(
      string = s"${p0.string} - $p1",
      long = p0.long + summon[Short],
      double = p2.getOrElse(0.1),
      enumeration = Enum.One
    ))

  override def method9(p0: String): Effect[String] = backend.failed(IllegalArgumentException(p0))

  protected def protectedMethod = ()

  private def privateMethod = ()

trait InvalidApi[Effect[_]]:

  def nomethod(test: String): Effect[Unit]

  def method1(test: String): Effect[Unit]

  def method2(test: String): Effect[String]

  def method3(p0: Short, p1: Seq[Int]): Effect[Int]

  def method4(p0: Long, p1: Byte, p2: String): Effect[String]

final case class InvalidApiImpl[Effect[_]](backend: Backend[Effect]) extends InvalidApi[Effect]:

  def nomethod(test: String): Effect[Unit] = backend.pure(())

  def method1(test: String): Effect[Unit] = backend.pure(())

  def method2(test: String): Effect[String] = backend.pure("")

  def method3(p0: Short, p1: Seq[Int]): Effect[Int] = backend.pure(0)

  def method4(p0: Long, p1: Byte, p2: String): Effect[String] = backend.pure("")
