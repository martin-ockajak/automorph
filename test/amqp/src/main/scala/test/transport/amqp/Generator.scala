package test.transport.amqp

import automorph.transport.amqp.Amqp
import java.time.Instant
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

object Generator {

  def context: Arbitrary[Amqp[_]] = Arbitrary(for {
    headers <- Gen.listOf(arbitrary[(String, String)].suchThat(_._1.nonEmpty)).map(_.toMap)
    deliveryMode <- Gen.option(Gen.choose(1, 2))
    priority <- Gen.option(Gen.choose(0, 9))
    correlationId <- arbitrary[Option[String]]
    expiration <- Gen.option(Gen.choose(0, Int.MaxValue).map(_.toString))
    messageId <- arbitrary[Option[String]]
    timestamp <- Gen.option(Gen.choose(0, Long.MaxValue).map(Instant.ofEpochMilli))
    `type` <- arbitrary[Option[String]]
    userId <- arbitrary[Option[String]]
    appId <- arbitrary[Option[String]]
  } yield Amqp(
    headers = headers,
    deliveryMode = deliveryMode,
    priority = priority,
    correlationId = correlationId,
    expiration = expiration,
    messageId = messageId,
    timestamp = timestamp,
    `type` = `type`,
    userId = userId,
    appId = appId
  ))
}
