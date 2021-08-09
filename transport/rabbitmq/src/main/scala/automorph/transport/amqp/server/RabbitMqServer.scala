package automorph.transport.amqp.server

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.amqp.{Amqp, RabbitMqCommon}
import automorph.util.Bytes
import automorph.util.Extensions.{ThrowableOps, TryOps}
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{AMQP, Address, BuiltinExchangeType, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope}
import java.io.IOException
import java.net.URL
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Using}

/**
 * RabbitMQ server transport plugin using AMQP as message transport protocol.
 *
 * The server interprets AMQP request message body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as outgoing AMQP response body.
 * AMQP request messages are consumed from the specified queues and automatically acknowledged.
 * AMQP response messages are published to default exchange using ''reply-to'' request property as routing key.
 *
 * @see [[https://www.rabbitmq.com/java-client.html Documentation]]
 * @see [[https://rabbitmq.github.io/rabbitmq-java-client/api/current/index.html API]]
 * @constructor Creates a RabbitMQ server transport plugin.
 * @param handler RPC request handler
 * @param runEffect effect execution function
 * @param url AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
 * @param queues names of non-durable exclusive queues to consume messages from
 * @param addresses broker hostnames and ports for reconnection attempts
 * @param connectionFactory AMQP broker connection factory
 * @tparam Effect effect type
 */
final case class RabbitMqServer[Effect[_]](
  handler: Handler.AnyCodec[Effect, Amqp[BasicProperties]],
  runEffect: Effect[Any] => Any,
  url: URL,
  queues: Seq[String],
  addresses: Seq[Address] = Seq.empty,
  connectionFactory: ConnectionFactory = new ConnectionFactory
)(implicit executionContext: ExecutionContext)
  extends Logging with ServerMessageTransport[Effect] {

  private lazy val connection = createConnection()
  private lazy val threadConsumer = RabbitMqCommon.threadLocalConsumer(connection, createConsumer)
  private val clientId = RabbitMqCommon.applicationId(getClass.getName)
  private val urlText = url.toExternalForm
  private val exchange = RabbitMqCommon.defaultDirectExchange
  private val system = handler.system

  override def close(): Effect[Unit] = system.wrap(connection.abort(AMQP.CONNECTION_FORCED, "Terminated"))

  private def createConsumer(channel: Channel): DefaultConsumer = {
    val consumer = new DefaultConsumer(channel) {

      override def handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: BasicProperties,
        body: Array[Byte]
      ): Unit = {
        logger.debug(
          "Received AMQP request",
          Map(
            "URL" -> urlText,
            "Routing key" -> envelope.getRoutingKey,
            "Correlation ID" -> properties.getCorrelationId,
            "Size" -> body.length
          )
        )

        // Process the request
        implicit val usingContext: Amqp[BasicProperties] = RabbitMqCommon.context(properties)
        runEffect(system.map(
          system.either(handler.processRequest(body)),
          (handlerResult: Either[Throwable, HandlerResult[Array[Byte]]]) =>
            handlerResult.fold(
              error => sendServerError(error, body, properties, envelope.getRoutingKey),
              result => {
                // Send the response
                val response = result.response.getOrElse(Array[Byte]())
                sendResponse(response, properties)
              }
            )
        ))
      }
    }
    queues.foreach { queue =>
      consumer.getChannel.basicConsume(queue, true, consumer)
    }
    consumer
  }

  private def sendServerError(
    error: Throwable,
    request: Array[Byte],
    properties: BasicProperties,
    routingKey: String
  ): Unit = {
    logger.debug(
      "Failed to process AMQP request",
      error,
      Map(
        "URL" -> urlText,
        "Routing key" -> routingKey,
        "Correlation ID" -> properties.getCorrelationId,
        "Size" -> request.length
      )
    )
    val message = Bytes.string.from(error.trace.mkString("\n")).unsafeArray
    sendResponse(message, properties)
  }

  private def sendResponse(message: Array[Byte], properties: BasicProperties): Unit = {
    logger.trace(
      "Sending AMQP request",
      Map(
        "URL" -> urlText,
        "Routing key" -> properties.getReplyTo,
        "Correlation ID" -> properties.getCorrelationId,
        "Size" -> message.length
      )
    )
    val consumer = threadConsumer.get
    Try {
      val routingKey = Option(properties.getReplyTo).getOrElse {
        throw new IllegalArgumentException("Missing request header: reply-to")
      }
      consumer.getChannel.basicPublish(exchange, routingKey, true, false, properties, message)
      logger.debug(
        "Sent AMQP request",
        Map(
          "URL" -> urlText,
          "Routing key" -> properties.getReplyTo,
          "Correlation ID" -> properties.getCorrelationId,
          "Size" -> message.length
        )
      )
    }.mapFailure { error =>
      logger.error(
        "Failed to send AMQP request",
        error,
        Map(
          "URL" -> urlText,
          "Routing key" -> properties.getReplyTo,
          "Correlation ID" -> properties.getCorrelationId,
          "Size" -> message.length
        )
      )
      error
    }.get
  }

  private def createConnection(): Connection = RabbitMqCommon.connect(url, Seq.empty, clientId, connectionFactory)
}
