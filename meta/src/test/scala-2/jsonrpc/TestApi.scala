package jsonrpc

trait TestApi[Effect[_]] {

  /** Test method. */
  def method0(): Effect[Unit]

  def method1(): Effect[Double]

  def method2(p0: String): Effect[Unit]

  def method3(p0: Float, p1: Long, p2: Option[Seq[Int]]): Effect[Seq[String]]

  def method4(p0: BigDecimal, p1: Byte, p2: Map[String, Int], p3: Option[String]): Effect[Long]

  def method5(p0: Boolean, p1: Short)(p2: List[Int]): Effect[Map[String, String]]

  def method6(p1: Double): Effect[Option[String]]

  def method7(p1: Boolean)(implicit context: Short): Effect[Int]

  def method8(p1: String, p2: Option[Double])(implicit context: Short): Effect[Record]

  def method9(p0: String): String

  protected def protectedMethod: String
}
