package test.transport.http

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import automorph.transport.http.Http

object Generator {
  private val maxSize = 256

  private val header = for {
    name <- Gen.alphaNumStr.suchThat(value => Range(1, maxSize).contains(value.length))
    value <- Gen.asciiPrintableStr.suchThat(_.length < maxSize)
  } yield (name, value)

  private val parameter = for {
// FIXME - restore
//    name <- Gen.alphaStr.suchThat(value => Range(1, maxSize).contains(value.length))
//    value <- Gen.asciiPrintableStr.suchThat(_.length < maxSize)
    name <- Gen.const("Test")
    value <- Gen.const("test")
  } yield (name, value)

  def context: Arbitrary[Http[_]] = Arbitrary(for {
    headers <- Gen.listOf(header)
    parameters <- Gen.listOf(parameter)
  } yield Http(
    headers = headers,
    parameters = parameters
  ))
}
