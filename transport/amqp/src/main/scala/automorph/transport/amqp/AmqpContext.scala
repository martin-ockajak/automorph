package automorph.transport.amqp

import java.time.Instant

/**
 * AMQP transport message context.
 *
 * Message transport plugins must use message context properties in the descending order of priority by source:
 * - This context
 * - Transport plugin context (this.transport)
 * - Default values
 *
 * @see [[https://www.rabbitmq.com/rebases/specs/amqp-xml-doc0-9-1.pdf AMQP specification]]
 * @param contentType MIME content type
 * @param contentEncoding MIME content encoding
 * @param headers message headers
 * @param deliveryMode non-persistent (1) or persistent (2)
 * @param priority message priority (0 to 9)
 * @param correlationId request-response correlation identifier
 * @param replyTo address to reply to
 * @param expiration message expiration specification (milliseconds)
 * @param messageId application message identifier
 * @param timestamp message timestamp
 * @param `type` message type name
 * @param userId user identifier
 * @param appId application identifier
 * @param transport specific transport plugin message context
 * @tparam Transport specific transport plugin message context type
 */
final case class AmqpContext[Transport](
  contentType: Option[String] = None,
  contentEncoding: Option[String] = None,
  headers: Map[String, Any] = Map.empty,
  deliveryMode: Option[Int] = None,
  priority: Option[Int] = None,
  correlationId: Option[String] = None,
  replyTo: Option[String] = None,
  expiration: Option[String] = None,
  messageId: Option[String] = None,
  timestamp: Option[Instant] = None,
  `type`: Option[String] = None,
  userId: Option[String] = None,
  appId: Option[String] = None,
  transport: Option[Transport] = None
)
