package automorph.transport.amqp.server

import automorph.Types
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.amqp.server.RabbitMqServer.Context
import automorph.transport.amqp.{Amqp, RabbitMqCommon, RabbitMqContext}
import automorph.util.Extensions.{ThrowableOps, TryOps}
import automorph.util.{Bytes, Random}
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{AMQP, Address, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope}
import java.net.URI
import scala.util.Try

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
  handler: Types.HandlerAnyCodec[Effect, Amqp[BasicProperties]],
  runEffect: Effect[Any] => Any,
  url: URI,
  queues: Seq[String],
  addresses: Seq[Address] = Seq.empty,
  connectionFactory: ConnectionFactory = new ConnectionFactory
) extends Logging with ServerMessageTransport[Effect] {

  private lazy val connection = createConnection()
  private lazy val threadConsumer = RabbitMqCommon.threadLocalConsumer(connection, createConsumer)
  private val clientId = RabbitMqCommon.applicationId(getClass.getName)
  private val urlText = url.toURL.toExternalForm
  private val exchange = RabbitMqCommon.defaultDirectExchange
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, RabbitMqServer.Context]]
  private val system = genericHandler.system

  override def close(): Effect[Unit] = system.wrap(connection.abort(AMQP.CONNECTION_FORCED, "Terminated"))

  private def createConsumer(channel: Channel): DefaultConsumer = {
    val consumer = new DefaultConsumer(channel) {

      override def handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        amqpProperties: BasicProperties,
        requestBody: Array[Byte]
      ): Unit = {
        val requestId = Option(amqpProperties.getCorrelationId).getOrElse(Random.id)
        lazy val requestProperties =
          RabbitMqCommon.messageProperties(requestId, envelope.getRoutingKey, urlText, Option(consumerTag))
        logger.debug("Received AMQP request", requestProperties)

        // Process the request
        implicit val usingContext: RabbitMqServer.Context = RabbitMqCommon.context(amqpProperties)
        runEffect(system.map(
          system.either(genericHandler.processRequest(requestBody, requestId, None)),
          (handlerResult: Either[Throwable, HandlerResult[Array[Byte], Context]]) =>
            handlerResult.fold(
              error => sendServerError(error, amqpProperties, requestProperties),
              result => {
                // Send the response
                val response = result.responseBody.getOrElse(Array[Byte]())
                sendResponse(response, amqpProperties, requestProperties)
              }
            )
        ))
        ()
      }
    }
    queues.foreach { queue =>
      consumer.getChannel.basicConsume(queue, true, consumer)
    }
    consumer
  }

  private def sendServerError(
    error: Throwable,
    amqpProperties: BasicProperties,
    requestProperties: => Map[String, String]
  ): Unit = {
    logger.debug("Failed to process AMQP request", error, requestProperties)
    val message = Bytes.string.from(error.trace.mkString("\n")).unsafeArray
    sendResponse(message, amqpProperties, requestProperties)
  }

  private def sendResponse(
    message: Array[Byte],
    amqpProperties: BasicProperties,
    requestProperties: => Map[String, String]
  ): Unit = {
    val replyTo = Option(amqpProperties.getReplyTo)
    lazy val responseProperties = replyTo.map { value =>
      requestProperties + (RabbitMqCommon.routingKeyProperty -> value)
    }.getOrElse(requestProperties - RabbitMqCommon.routingKeyProperty)
    logger.trace("Sending AMQP response", responseProperties)
    val consumer = threadConsumer.get
    Try {
      val routingKey = replyTo.getOrElse {
        throw new IllegalArgumentException("Missing request header: reply-to")
      }
      consumer.getChannel.basicPublish(exchange, routingKey, true, false, amqpProperties, message)
      logger.debug("Sent AMQP response", responseProperties)
    }.onFailure(logger.error("Failed to send AMQP response", _, responseProperties)).get
  }

  private def createConnection(): Connection = RabbitMqCommon.connect(url, Seq.empty, clientId, connectionFactory)
}

object RabbitMqServer {

  /** Request context type. */
  type Context = Amqp[RabbitMqContext]
}
