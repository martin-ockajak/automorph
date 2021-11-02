package automorph.transport.amqp.client

import automorph.log.Logging
import automorph.spi.EffectSystem
import automorph.spi.system.{Defer, Deferred}
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.amqp.client.RabbitMqClient.{Context, Response}
import automorph.transport.amqp.{AmqpContext, RabbitMqCommon, RabbitMqContext}
import automorph.util.Bytes
import automorph.util.Extensions.{EffectOps, TryOps}
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{Address, BuiltinExchangeType, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope}
import java.net.URI
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.ArraySeq
import scala.util.{Try, Using}

/**
 * RabbitMQ client message transport plugin.
 *
 * The client uses the supplied RPC request as AMQP request message body and returns AMQP response message body as a result.
 * AMQP request messages are published to the specified exchange using ``direct reply-to``mechanism.
 * AMQP response messages are consumed using ``direct reply-to``mechanism and automatically acknowledged.
 *
 * @see [[https://www.rabbitmq.com/java-client.html Documentation]]
 * @see [[https://rabbitmq.github.io/rabbitmq-java-client/api/current/index.html API]]
 * @constructor Creates a RabbitMQ client message transport plugin.
 * @param url AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
 * @param routingKey AMQP routing key (typically a queue name)
 * @param system effect system plugin
 * @param exchange direct non-durable AMQP message exchange name
 * @param addresses broker hostnames and ports for reconnection attempts
 * @param connectionFactory AMQP broker connection factory
 * @tparam Effect effect type
 */
final case class RabbitMqClient[Effect[_]](
  url: URI,
  routingKey: String,
  system: EffectSystem[Effect] with Defer[Effect],
  exchange: String = RabbitMqCommon.defaultDirectExchange,
  addresses: Seq[Address] = Seq.empty,
  connectionFactory: ConnectionFactory = new ConnectionFactory
) extends Logging with ClientMessageTransport[Effect, Context] {

  private lazy val connection = createConnection()
  private lazy val threadConsumer = RabbitMqCommon.threadLocalConsumer(connection, createConsumer)
  private val clientId = RabbitMqCommon.applicationId(getClass.getName)
  private val urlText = url.toURL.toExternalForm
  private val responseHandlers = TrieMap[String, Deferred[Effect, Response]]()
  private val directReplyToQueue = "amq.rabbitmq.reply-to"
  implicit private val givenSystem: EffectSystem[Effect] = system

  override def call(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    requestContext: Option[Context]
  ): Effect[Response] =
    system.deferred[Response].flatMap { response =>
      send(requestBody, requestId, mediaType, requestContext, Some(response)).flatMap(_ => response.effect)
    }

  override def notify(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    requestContext: Option[Context]
  ): Effect[Unit] = send(requestBody, requestId, mediaType, requestContext, None)

  override def defaultContext: Context = RabbitMqContext.default

  override def close(): Effect[Unit] = system.wrap(RabbitMqCommon.disconnect(connection))

  private def send(
    requestBody: ArraySeq.ofByte,
    defaultRequestId: String,
    mediaType: String,
    requestContext: Option[Context],
    response: Option[Deferred[Effect, Response]]
  ): Effect[Unit] = {
    // Log the request
    val amqpProperties =
      RabbitMqCommon.amqpProperties(requestContext, mediaType, directReplyToQueue, defaultRequestId, clientId)
    val requestId = amqpProperties.getCorrelationId
    lazy val requestProperties = RabbitMqCommon.extractProperties(requestId, routingKey, urlText, None)
    logger.trace("Sending AMQP request", requestProperties)

    // Register deferred response effect if available
    response.foreach(responseHandlers.put(requestId, _))

    // Send the request
    system.wrap {
      Try {
        val message = requestBody.unsafeArray
        threadConsumer.get.getChannel.basicPublish(exchange, routingKey, true, false, amqpProperties, message)
        logger.debug("Sent AMQP request", requestProperties)
      }.onFailure(logger.error("Failed to send AMQP request", _, requestProperties)).get
    }
  }

  private def createConsumer(channel: Channel): DefaultConsumer = {
    val consumer = new DefaultConsumer(channel) {

      override def handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: BasicProperties,
        responseBody: Array[Byte]
      ): Unit = {
        // Log the response
        lazy val responseProperties =
          RabbitMqCommon.extractProperties(properties.getCorrelationId, routingKey, urlText, None)
        logger.debug("Received AMQP response", responseProperties)

        // Resolve the registered deferred response effect
        val responseContext = RabbitMqCommon.context(properties)
        responseHandlers.get(properties.getCorrelationId).foreach { response =>
          response.succeed(Bytes.byteArray.from(responseBody) -> responseContext).run
        }
      }
    }
    consumer.getChannel.basicConsume(directReplyToQueue, true, consumer)
    consumer
  }

  private def createConnection(): Connection = {
    val connection = RabbitMqCommon.connect(url, Seq.empty, clientId, connectionFactory)
    Option.when(exchange != RabbitMqCommon.defaultDirectExchange) {
      Using(connection.createChannel()) { channel =>
        channel.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT, false)
        connection
      }.get
    }.getOrElse(connection)
  }
}

object RabbitMqClient {

  /** Request context type. */
  type Context = AmqpContext[RabbitMqContext]

  private type Response = (ArraySeq.ofByte, Context)
}
