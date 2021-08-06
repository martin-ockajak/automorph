package test.transport.amqp

import automorph.transport.amqp.Amqp
import java.time.Instant
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

object Generator {
  def context[Source] = Arbitrary(for {
    headers <- arbitrary[Map[String, String]]
    deliveryMode <- Gen.option(Gen.choose(1, 2))
    priority <- Gen.option(Gen.choose(0, 9))
    correlationId <- arbitrary[Option[String]]
    expiration <- Gen.option(Gen.choose(0, Int.MaxValue))
    messageId <- arbitrary[Option[String]]
    timestamp <- Gen.option(Gen.choose(0, Long.MaxValue).map(Instant.ofEpochMilli))
    `type` <- arbitrary[Option[String]]
    userId <- arbitrary[Option[String]]
    appId <- arbitrary[Option[String]]
  } yield Amqp[Source](
    None,
    None,
    None,
    headers = headers,
    deliveryMode = deliveryMode,
    priority = priority,
    correlationId = correlationId,
    `type` = `type`,
    userId = userId,
    appId = appId
  ))
}
