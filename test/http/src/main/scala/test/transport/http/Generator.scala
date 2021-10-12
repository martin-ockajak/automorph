package test.transport.http

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import automorph.transport.http.Http

object Generator {

  private val header = for {
    name <- Gen.alphaStr.suchThat(_.nonEmpty)
    value <- Gen.asciiPrintableStr
  } yield (name, value)

  private val parameter = for {
    name <- arbitrary[String].suchThat(_.nonEmpty)
    value <- arbitrary[String]
  } yield (name, value)

  def context: Arbitrary[Http[_]] = Arbitrary(for {
    headers <- Gen.listOf(header)
    parameters <- Gen.listOf(parameter)
  } yield Http(
    headers = headers,
    parameters = parameters
  ))
}
