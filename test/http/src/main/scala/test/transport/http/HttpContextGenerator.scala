package test.transport.http

import automorph.transport.http.HttpContext
import org.scalacheck.{Arbitrary, Gen}

object HttpContextGenerator {
  private val maxSize = 256

  private val header = for {
// FIXME - restore
//    name <- Gen.alphaNumStr.suchThat(value => Range(1, maxSize).contains(value.length))
    name <- Gen.const("Test")
//    value <- Gen.const("test")
    value <- Gen.asciiPrintableStr.suchThat(_.length < maxSize)
  } yield (name, value)

  private val parameter = for {
// FIXME - restore
//    name <- Gen.alphaStr.suchThat(value => Range(1, maxSize).contains(value.length))
//    value <- Gen.asciiPrintableStr.suchThat(_.length < maxSize)
    name <- Gen.const("Test")
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
