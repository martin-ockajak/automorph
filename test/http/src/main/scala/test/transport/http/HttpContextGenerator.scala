package test.transport.http

import automorph.transport.http.HttpContext
import org.scalacheck.{Arbitrary, Gen}

object HttpContextGenerator {
  private val maxNameSize = 64
  private val maxValueSize = 256

  private val header = for {
    name <- Gen.choose(0, maxNameSize).flatMap(size => Gen.stringOfN(size, Gen.alphaNumChar))
    value <- Gen.asciiPrintableStr.suchThat(_.length < maxValueSize)
  } yield (name, value)

  private val parameter = for {
//    name <- Gen.alphaStr.suchThat(value => Range(1, maxSize).contains(value.length))
//    value <- Gen.asciiPrintableStr.suchThat(_.length < maxSize)
//    name <- Gen.choose(1, 16).flatMap(size => Gen.stringOfN(size, Gen.alphaNumChar))
    name <- Gen.const("test")
    value <- Gen.const("test")
  } yield (name, value)

  def arbitrary[T]: Arbitrary[HttpContext[T]] = Arbitrary(for {
    headers <- Gen.listOf(header)
    parameters <- Gen.listOf(parameter)
  } yield HttpContext(
    headers = headers,
    parameters = parameters
  ))
}
