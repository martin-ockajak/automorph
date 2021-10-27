package test.transport.http

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import automorph.transport.http.Http

object Generator {

  private val header = for {
    name <- Gen.alphaNumStr.suchThat(_.nonEmpty)
    value <- Gen.asciiPrintableStr
  } yield (name, value)

  private val parameter = for {
// FIXME - restore
//    name <- arbitrary[String].suchThat(_.nonEmpty)
//    value <- arbitrary[String]
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
