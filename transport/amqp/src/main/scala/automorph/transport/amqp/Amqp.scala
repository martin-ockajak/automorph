package automorph.transport.amqp

import java.time.Instant

/**
 * AMQP message properties.
 *
 * @see [[https://www.rabbitmq.com/resources/specs/amqp-xml-doc0-9-1.pdf AMQP specification]]
 * @param source source properties provided by the specific message transport plugin
 * @param contentType MIME content type
 * @param contentEncoding MIME content encoding
 * @param headers message headers
 * @param deliveryMode non-persistent (1) or persistent (2)
 * @param priority message priority (0 to 9)
 * @param correlationId request-response correlation identifier
 * @param replyTo address to reply to
 * @param expiration message expiration specification
 * @param messageId application message identifier
 * @param timestamp message timestamp
 * @param `type` message type name
 * @param userId creating user identifier
 * @param appId creating application identifier
 * @tparam Source specific message transport plugin source properties type
 */
final case class Amqp[Source](
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
