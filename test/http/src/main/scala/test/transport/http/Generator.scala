package test.transport.http

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import automorph.transport.http.Http

object Generator {

  def context[Source] = Arbitrary(for {
    headers <- Gen.listOf(arbitrary[(String, String)].suchThat(_._1.nonEmpty))
    parameters <- Gen.listOf(arbitrary[(String, String)].suchThat(_._1.nonEmpty))
  } yield Http[Source](
    headers = headers,
    parameters = parameters
  ))
}
