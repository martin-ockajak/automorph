package test.transport.amqp

import automorph.transport.amqp.AmqpContext
import java.time.Instant
import org.scalacheck.{Arbitrary, Gen}

object AmqpContextGenerator {

  def arbitrary[T]: Arbitrary[AmqpContext[T]] = Arbitrary(for {
    headers <- Gen.listOf(Arbitrary.arbitrary[(String, String)].suchThat(_._1.nonEmpty)).map(_.toMap)
    deliveryMode <- Gen.option(Gen.choose(1, 2))
    priority <- Gen.option(Gen.choose(0, 9))
    correlationId <- Arbitrary.arbitrary[Option[String]]
    expiration <- Gen.option(Gen.choose(0, Int.MaxValue).map(_.toString))
    messageId <- Arbitrary.arbitrary[Option[String]]
    timestamp <- Gen.option(Gen.choose(0L, Long.MaxValue).map(Instant.ofEpochMilli))
    `type` <- Arbitrary.arbitrary[Option[String]]
    userId <- Arbitrary.arbitrary[Option[String]]
    appId <- Arbitrary.arbitrary[Option[String]]
  } yield AmqpContext(
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
