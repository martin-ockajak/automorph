package automorph.transport.amqp.client

import automorph.log.Logging
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.amqp.client.RabbitMqClient.Context
import automorph.transport.amqp.{Amqp, RabbitMqCommon}
import automorph.util.Extensions.TryOps
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{AMQP, Address, BuiltinExchangeType, Channel, Connection, ConnectionFactory, Consumer, DefaultConsumer, Envelope}
import java.net.URI
import java.util.Date
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.ArraySeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.{Random, Try, Using}

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
  promisedEffect: () => (Effect[ArraySeq.ofByte], ArraySeq.ofByte => Unit),
  exchange: String = RabbitMqCommon.defaultDirectExchange,
  addresses: Seq[Address] = Seq.empty,
  connectionFactory: ConnectionFactory = new ConnectionFactory
) extends Logging with ClientMessageTransport[Effect, Context] {

  private lazy val connection = createConnection()
  private lazy val threadConsumer = RabbitMqCommon.threadLocalConsumer(connection, createConsumer)
  private val clientId = RabbitMqCommon.applicationId(getClass.getName)
  private val deliveryHandlers = TrieMap[String, ArraySeq.ofByte => Unit]()
  private val directReplyToQueue = "amq.rabbitmq.reply-to"
  private lazy val random = new Random(System.currentTimeMillis() + Runtime.getRuntime.totalMemory())

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): Effect[ArraySeq.ofByte] = {
    val properties = createProperties(mediaType, context)
    val (result, complete) = promisedEffect()
    system.flatMap(send(request, properties, Some(complete)), _ => result)
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[Unit] = {
    val properties = createProperties(mediaType, context)
    send(request, properties, None)
  }

  override def defaultContext: Context = RabbitMqClient.defaultContext

  override def close(): Effect[Unit] = system.wrap(RabbitMqCommon.disconnect(connection))

  private def send(
    request: ArraySeq.ofByte,
    properties: BasicProperties,
    complete: Option[ArraySeq.ofByte => Unit]
  ): Effect[Unit] = {
    logger.trace(
      "Sending AMQP request",
      Map(
        "URL" -> url,
        "Routing key" -> routingKey,
        "Correlation ID" -> properties.getCorrelationId,
        "Size" -> request.length
      )
    )
    val consumer = threadConsumer.get

    // Retain result promise if available
    complete.foreach(deliveryHandlers.put(properties.getCorrelationId, _))

    // Send the request
    blockingEffect(() => Try {
      consumer.getChannel.basicPublish(exchange, routingKey, true, false, properties, request.unsafeArray)
      logger.debug(
        "Sent AMQP request",
        Map(
          "URL" -> url,
          "Routing key" -> routingKey,
          "Correlation ID" -> properties.getCorrelationId,
          "Size" -> request.length
        )
      )
    }.mapFailure { error =>
      logger.error(
        "Failed to send AMQP request",
        error,
        Map(
          "URL" -> url,
          "Routing key" -> routingKey,
          "Correlation ID" -> properties.getCorrelationId,
          "Size" -> request.length
        )
      )
      error
    }.get)
  }

  private def createProperties(mediaType: String, context: Option[Context]): BasicProperties = {
    val properties = context.getOrElse(defaultContext)
    properties.source.getOrElse(new BasicProperties).builder()
      .replyTo(properties.replyTo.getOrElse(directReplyToQueue))
      .correlationId(properties.correlationId.getOrElse(Math.abs(random.nextLong()).toString))
      .contentType(properties.contentType.getOrElse(mediaType))
      .appId(properties.appId.getOrElse(clientId))
      .contentEncoding(properties.contentEncoding.orNull)
      .headers(properties.headers.asJava)
      .deliveryMode(properties.deliveryMode.map(new Integer(_)).orNull)
      .priority(properties.priority.map(new Integer(_)).orNull)
      .expiration(properties.expiration.orNull)
      .messageId(properties.messageId.orNull)
      .timestamp(properties.timestamp.map(Date.from).orNull)
      .`type`(properties.`type`.orNull)
      .userId(properties.userId.orNull)
      .build
  }

  private def createConsumer(channel: Channel): DefaultConsumer = {
    val consumer = new DefaultConsumer(channel) {

      override def handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: BasicProperties,
        body: Array[Byte]
      ): Unit = {
        logger.debug(
          "Received AMQP response",
          Map(
            "URL" -> url,
            "Routing key" -> routingKey,
            "Correlation ID" -> properties.getCorrelationId,
            "Size" -> body.length
          )
        )
        deliveryHandlers.get(properties.getCorrelationId).foreach { complete =>
          complete(new ArraySeq.ofByte(body))
        }
      }
    }
    consumer.getChannel.basicConsume(directReplyToQueue, true, consumer)
    consumer
  }

  private def createConnection(): Connection = {
    val connection = RabbitMqCommon.connect(url.toURL, Seq.empty, clientId, connectionFactory)
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
  type Context = Amqp[BasicProperties]

  implicit val defaultContext: Context = Amqp(source = Some(new BasicProperties))

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
      val promise = Promise[ArraySeq.ofByte]
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
