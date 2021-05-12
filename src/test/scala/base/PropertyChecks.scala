package base

import org.scalacheck.{Gen, Prop}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

trait PropertyChecks extends Matchers:

  def check(property: Prop): Assertion =
    val result = property.apply(Gen.Parameters.default)
    if result.failure then
      fail(new AssertionError(result.status.toString))
    else
      succeed
