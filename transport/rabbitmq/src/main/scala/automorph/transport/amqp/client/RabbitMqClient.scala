package automorph.transport.amqp.client

import automorph.log.Logging
import automorph.spi.ClientMessageTransport
import automorph.transport.amqp.RabbitMqCommon
import automorph.transport.amqp.client.RabbitMqClient.RequestProperties
import automorph.util.Extensions.TryOps
import automorph.util.MessageId
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{AMQP, Address, BuiltinExchangeType, Channel, Connection, ConnectionFactory, Consumer, DefaultConsumer, Envelope}
import java.io.IOException
import java.net.URL
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.Duration
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
 * @param exchange direct non-durable AMQP message exchange name
 * @param addresses broker hostnames and ports for reconnection attempts
 * @param connectionFactory AMQP broker connection factory
 * @param executionContext execution context
 */
final case class RabbitMqClient(
  url: URL,
  routingKey: String,
  exchange: String = RabbitMqCommon.defaultDirectExchange,
  addresses: Seq[Address] = Seq(),
  connectionFactory: ConnectionFactory = new ConnectionFactory
)(implicit executionContext: ExecutionContext)
  extends AutoCloseable with Logging with ClientMessageTransport[Future, RequestProperties] {

  private lazy val connection = createConnection()
  private lazy val threadConsumer = RabbitMqCommon.threadLocalConsumer(connection, createConsumer)
  private val clientId = RabbitMqCommon.applicationId(getClass.getName)
  private val urlText = url.toExternalForm
  private val callResults = TrieMap[String, Promise[ArraySeq.ofByte]]()
  private val directReplyToQueue = "amq.rabbitmq.reply-to"

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): Future[ArraySeq.ofByte] = {
    val properties = setupProperties(mediaType, context)
    val result = Promise[ArraySeq.ofByte]()
    send(request, properties, Some(result)).flatMap(_ => result.future)
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[RequestProperties]): Future[Unit] = {
    val properties = setupProperties(mediaType, context)
    send(request, properties, None)
  }

  override def defaultContext: RequestProperties = RequestProperties.defaultContext

  override def close(): Unit = RabbitMqCommon.disconnect(connection)

  private def send(
    request: ArraySeq.ofByte,
    properties: BasicProperties,
    result: Option[Promise[ArraySeq.ofByte]]
  ): Future[Unit] = Future {
    logger.trace(
      "Sending AMQP request",
      Map(
        "URL" -> urlText,
        "Routing key" -> routingKey,
        "Correlation ID" -> properties.getCorrelationId,
        "Size" -> request.length
      )
    )
    val consumer = threadConsumer.get

    // Retain result promise if available
    result.foreach(callResults.put(properties.getCorrelationId, _))

    // Send the request
    Try {
      consumer.getChannel.basicPublish(exchange, routingKey, true, false, properties, request.unsafeArray)
      logger.debug(
        "Sent AMQP request",
        Map(
          "URL" -> urlText,
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
          "URL" -> urlText,
          "Routing key" -> routingKey,
          "Correlation ID" -> properties.getCorrelationId,
          "Size" -> request.length
        )
      )
      error
    }.get
  }

  private def setupProperties(mediaType: String, context: Option[RequestProperties]): BasicProperties =
    context.getOrElse(defaultContext).basic.builder().replyTo(directReplyToQueue)
      .correlationId(MessageId.next).contentType(mediaType).appId(clientId).build

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
            "URL" -> urlText,
            "Routing key" -> routingKey,
            "Correlation ID" -> properties.getCorrelationId,
            "Size" -> body.length
          )
        )
        callResults.get(properties.getCorrelationId).foreach { promise =>
          promise.success(new ArraySeq.ofByte(body))
        }
      }
    }
    consumer.getChannel.basicConsume(directReplyToQueue, true, consumer)
    consumer
  }

  private def createConnection(): Connection = {
    val connection = RabbitMqCommon.connect(url, Seq(), clientId, connectionFactory)
    Option.when(exchange != RabbitMqCommon.defaultDirectExchange) {
      Using(connection.createChannel()) { channel =>
        channel.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT, false)
        connection
      }.get
    }.getOrElse(connection)
  }
}

case object RabbitMqClient {

  case class RequestProperties(
    basic: BasicProperties
  )

  case object RequestProperties {
    implicit val defaultContext: RequestProperties = RequestProperties(new BasicProperties)
  }
}
