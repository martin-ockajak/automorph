package test.transport.http

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import automorph.transport.http.Http

object Generator {
  def context[Source] = Arbitrary(for {
    headers <- arbitrary[Seq[(String, String)]]
  } yield Http[Source](headers = headers))
}
