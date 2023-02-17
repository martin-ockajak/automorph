package automorph.transport.amqp.client

import automorph.log.{Logging, MessageLog}
import automorph.spi.EffectSystem
import automorph.spi.system.{Completable, CompletableEffectSystem}
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.amqp.client.RabbitMqClient.{Context, Response}
import automorph.transport.amqp.{AmqpContext, RabbitMqCommon, RabbitMqContext}
import automorph.util.Extensions.{ByteArrayOps, EffectOps, InputStreamOps, TryOps}
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{Address, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope}
import java.io.InputStream
import java.net.URI
import scala.collection.concurrent.TrieMap
import scala.util.Try

/**
 * RabbitMQ client message transport plugin.
 *
 * The client uses the supplied RPC request as AMQP request message body and returns AMQP response message body as a
 * result. AMQP request messages are published to the specified exchange using ``direct reply-to``mechanism. AMQP
 * response messages are consumed using ``direct reply-to``mechanism and automatically acknowledged.
 *
 * @see
 *   [[https://www.rabbitmq.com/java-client.html Documentation]]
 * @see
 *   [[https://rabbitmq.github.io/rabbitmq-java-client/api/current/index.html API]]
 * @constructor
 *   Creates a RabbitMQ client message transport plugin.
 * @param url
 *   AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
 * @param routingKey
 *   AMQP routing key (typically a queue name)
 * @param system
 *   effect system plugin
 * @param exchange
 *   direct non-durable AMQP message exchange name
 * @param addresses
 *   broker hostnames and ports for reconnection attempts
 * @param connectionFactory
 *   AMQP broker connection factory
 * @tparam Effect
 *   effect type
 */
final case class RabbitMqClient[Effect[_]](
  url: URI,
  routingKey: String,
  system: CompletableEffectSystem[Effect],
  exchange: String = RabbitMqCommon.defaultDirectExchange,
  addresses: Seq[Address] = Seq.empty,
  connectionFactory: ConnectionFactory = new ConnectionFactory,
) extends Logging with ClientMessageTransport[Effect, Context] {
  private lazy val connection = connect()
  private lazy val threadConsumer = RabbitMqCommon.threadLocalConsumer(connection, consumer)
  private val directReplyToQueue = "amq.rabbitmq.reply-to"
  private val clientId = RabbitMqCommon.applicationId(getClass.getName)
  private val urlText = url.toString
  private val responseHandlers = TrieMap[String, Completable[Effect, Response]]()
  private val log = MessageLog(logger, RabbitMqCommon.protocol)
  implicit private val givenSystem: EffectSystem[Effect] = system

  override def call(
    requestBody: InputStream,
    requestContext: Option[Context],
    requestId: String,
    mediaType: String,
  ): Effect[Response] =
    system.completable[Response].flatMap { response =>
      send(requestBody, requestId, mediaType, requestContext, Some(response)).flatMap(_ => response.effect)
    }

  private def send(
    requestBody: InputStream,
    defaultRequestId: String,
    mediaType: String,
    requestContext: Option[Context],
    response: Option[Completable[Effect, Response]],
  ): Effect[Unit] = {
    // Log the request
    val amqpProperties = RabbitMqCommon.amqpProperties(
      requestContext,
      mediaType,
      directReplyToQueue,
      defaultRequestId,
      clientId,
      useDefaultRequestId = false,
    )
    val requestId = amqpProperties.getCorrelationId
    lazy val requestProperties = RabbitMqCommon.messageProperties(Some(requestId), routingKey, urlText, None)
    log.sendingRequest(requestProperties)

    // Register deferred response effect if available
    response.foreach(responseHandlers.put(requestId, _))

    // Send the request
    system.evaluate {
      Try {
        val message = requestBody.toArray
        threadConsumer.get.getChannel.basicPublish(exchange, routingKey, true, false, amqpProperties, message)
        log.sentRequest(requestProperties)
      }.onFailure(error => log.failedSendRequest(error, requestProperties)).get
    }
  }

  override def message(
    requestBody: InputStream,
    requestContext: Option[Context],
    requestId: String,
    mediaType: String,
  ): Effect[Unit] =
    send(requestBody, requestId, mediaType, requestContext, None)

  override def defaultContext: Context =
    RabbitMqContext.default

  override def close(): Effect[Unit] =
    system.evaluate(RabbitMqCommon.disconnect(connection))

  private def consumer(channel: Channel): DefaultConsumer = {
    val consumer = new DefaultConsumer(channel) {

      override def handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: BasicProperties,
        responseBody: Array[Byte],
      ): Unit = {
        // Log the response
        lazy val responseProperties = RabbitMqCommon
          .messageProperties(Option(properties.getCorrelationId), routingKey, urlText, None)
        log.receivedResponse(responseProperties)

        // Complete the registered deferred response effect
        val responseContext = RabbitMqCommon.messageContext(properties)
        responseHandlers.get(properties.getCorrelationId).foreach { response =>
          response.succeed(responseBody.toInputStream -> responseContext).fork
        }
      }
    }
    consumer.getChannel.basicConsume(directReplyToQueue, true, consumer)
    consumer
  }

  private def connect(): Connection = {
    val connection = RabbitMqCommon.connect(url, addresses, clientId, connectionFactory)
    RabbitMqCommon.declareExchange(exchange, connection)
    connection
  }
}

object RabbitMqClient {

  /** Request context type. */
  type Context = AmqpContext[RabbitMqContext]

  private type Response = (InputStream, Context)
}
