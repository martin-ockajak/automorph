package automorph.transport.amqp.client

import automorph.log.Logging
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.amqp.client.RabbitMqClient.Context
import automorph.transport.amqp.{Amqp, RabbitMqCommon, RabbitMqContext}
import automorph.util.Bytes
import automorph.util.Extensions.TryOps
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{Address, BuiltinExchangeType, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope}
import java.net.URI
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Try, Using}

/**
 * RabbitMQ client transport plugin using AMQP as message transport protocol.
 *
 * The client uses the supplied RPC request as AMQP request message body and returns AMQP response message body as a result.
 * AMQP request messages are published to the specified exchange using ``direct reply-to``mechanism.
 * AMQP response messages are consumed using ``direct reply-to``mechanism and automatically acknowledged.
 *
 * @see [[https://www.rabbitmq.com/java-client.html Documentation]]
 * @see [[https://rabbitmq.github.io/rabbitmq-java-client/api/current/index.html API]]
 * @constructor Creates a RabbitMQ client transport plugin.
 * @param url AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
 * @param routingKey AMQP routing key (typically a queue name)
 * @param system effect system plugin
 * @param blockingEffect creates an effect from specified blocking function
 * @param promisedEffect creates a not yet completed effect and its completion function
 * @param exchange direct non-durable AMQP message exchange name
 * @param addresses broker hostnames and ports for reconnection attempts
 * @param connectionFactory AMQP broker connection factory
 * @tparam Effect effect type
 */
final case class RabbitMqClient[Effect[_]](
  url: URI,
  routingKey: String,
  system: EffectSystem[Effect],
  blockingEffect: (() => Unit) => Effect[Unit],
  promisedEffect: () => (Effect[Any], Any => Unit),
  exchange: String = RabbitMqCommon.defaultDirectExchange,
  addresses: Seq[Address] = Seq.empty,
  connectionFactory: ConnectionFactory = new ConnectionFactory
) extends Logging with ClientMessageTransport[Effect, Context] {

  private lazy val connection = createConnection()
  private lazy val threadConsumer = RabbitMqCommon.threadLocalConsumer(connection, createConsumer)
  private val clientId = RabbitMqCommon.applicationId(getClass.getName)
  private val urlText = url.toURL.toExternalForm
  private val responseHandlers = TrieMap[String, Any => Unit]()
  private val directReplyToQueue = "amq.rabbitmq.reply-to"

  override def call(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    requestContext: Option[Context]
  ): Effect[(ArraySeq.ofByte, Context)] = {
    val (effectResult, completeEffect) = promisedEffect()
    system.flatMap(
      send(requestBody, requestId, mediaType, requestContext, Some(completeEffect)),
      (_: Unit) => effectResult.asInstanceOf[Effect[(ArraySeq.ofByte, Context)]]
    )
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
    completeEffect: Option[Any => Unit]
  ): Effect[Unit] = {
    // Log the request
    val amqpProperties =
      RabbitMqCommon.amqpProperties(requestContext, mediaType, directReplyToQueue, defaultRequestId, clientId)
    val requestId = amqpProperties.getCorrelationId
    lazy val requestProperties = RabbitMqCommon.extractProperties(requestId, routingKey, urlText, None)
    logger.trace("Sending AMQP request", requestProperties)

    // Register response processing promise if available
    completeEffect.foreach(responseHandlers.put(requestId, _))

    // Send the request
    blockingEffect(() =>
      Try {
        val message = requestBody.unsafeArray
        threadConsumer.get.getChannel.basicPublish(exchange, routingKey, true, false, amqpProperties, message)
        logger.debug("Sent AMQP request", requestProperties)
      }.onFailure(logger.error("Failed to send AMQP request", _, requestProperties)).get
    )
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

        // Fulfill the registered response processing promise
        val responseContext = RabbitMqCommon.context(properties)
        responseHandlers.get(properties.getCorrelationId).foreach { complete =>
          complete(Bytes.byteArray.from(responseBody) -> responseContext)
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
  type Context = Amqp[RabbitMqContext]

  /**
   * Creates asynchronous RabbitMQ client transport plugin.
   *
   * @see [[https://www.rabbitmq.com/java-client.html Documentation]]
   * @see [[https://rabbitmq.github.io/rabbitmq-java-client/api/current/index.html API]]
   * @param url AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
   * @param routingKey AMQP routing key (typically a queue name)
   * @param exchange direct non-durable AMQP message exchange name
   * @param addresses broker hostnames and ports for reconnection attempts
   * @param connectionFactory AMQP broker connection factory
   * @param executionContext execution context
   */
  def async(
    url: URI,
    routingKey: String,
    exchange: String = RabbitMqCommon.defaultDirectExchange,
    addresses: Seq[Address] = Seq.empty,
    connectionFactory: ConnectionFactory = new ConnectionFactory
  )(implicit executionContext: ExecutionContext): RabbitMqClient[Future] = {
    val blockingEffect = (blocking: () => Unit) => Future(blocking())
    val promisedEffect = () => {
      val promise = Promise[Any]()
      promise.future -> promise.success.andThen(_ => ())
    }
    RabbitMqClient(
      url,
      routingKey,
      FutureSystem(),
      blockingEffect,
      promisedEffect,
      exchange,
      addresses,
      connectionFactory
    )
  }
}
