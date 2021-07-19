package automorph.transport.amqp

import java.time.Instant

final case class AmqpProperties[Source](
  source: Option[Source] = None,
  contentType: Option[String] = None,
  contentEncoding: Option[String] = None,
  headers: Map[String, Any] = Map(),
  deliveryMode: Option[Int] = None,
  priority: Option[Int] = None,
  correlationId: Option[String] = None,
  replyTo: Option[String] = None,
  expiration: Option[String] = None,
  messageId: Option[String] = None,
  timestamp: Option[Instant] = None,
  `type`: Option[String] = None,
  userId: Option[String] = None,
  appId: Option[String] = None
)
