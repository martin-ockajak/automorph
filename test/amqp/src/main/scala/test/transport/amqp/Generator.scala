package test.transport.amqp

import automorph.transport.amqp.Amqp
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

object Generator {
  def context[Source] = Arbitrary(for {
    headers <- arbitrary[Map[String, String]]
  } yield Amqp[Source](headers = headers))
}
