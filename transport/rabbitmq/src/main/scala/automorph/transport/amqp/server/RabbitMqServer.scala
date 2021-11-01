package automorph.transport.amqp.server

import automorph.Types
import automorph.log.Logging
import automorph.spi.EffectSystem
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.amqp.server.RabbitMqServer.{Context, Run}
import automorph.transport.amqp.{AmqpContext, RabbitMqCommon, RabbitMqContext}
import automorph.util.Extensions.{EffectOps, ThrowableOps, TryOps}
import automorph.util.{Bytes, Random}
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{AMQP, Address, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope}
import java.net.URI
import scala.util.Try

/**
 * RabbitMQ server message transport plugin.
 *
 * The server interprets AMQP request message body as an RPC request and processes it using the specified RPC request handler.
 * The response returned by the RPC request handler is used as outgoing AMQP response body.
 * AMQP request messages are consumed from the specified queues and automatically acknowledged.
 * AMQP response messages are published to default exchange using ''reply-to'' request property as routing key.
 *
 * @see [[https://www.rabbitmq.com/java-client.html Documentation]]
 * @see [[https://rabbitmq.github.io/rabbitmq-java-client/api/current/index.html API]]
 * @constructor Creates a RabbitMQ server message transport plugin.
 * @param handler RPC request handler
 * @param url AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
 * @param queues names of non-durable exclusive queues to consume messages from
 * @param addresses broker hostnames and ports for reconnection attempts
 * @param connectionFactory AMQP broker connection factory
 * @param runEffect effect execution function
 * @tparam Effect effect type
 */
final case class RabbitMqServer[Effect[_]] private (
  handler: Types.HandlerAnyCodec[Effect, AmqpContext[BasicProperties]],
  url: URI,
  queues: Seq[String],
  addresses: Seq[Address],
  connectionFactory: ConnectionFactory,
  runEffect: Run[Effect]
) extends Logging with ServerMessageTransport[Effect] {

  private lazy val connection = createConnection()
  private lazy val threadConsumer = RabbitMqCommon.threadLocalConsumer(connection, createConsumer)
  private val serverId = RabbitMqCommon.applicationId(getClass.getName)
  private val urlText = url.toURL.toExternalForm
  private val exchange = RabbitMqCommon.defaultDirectExchange
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, RabbitMqServer.Context]]
  private val system = genericHandler.system
  implicit private val givenSystem: EffectSystem[Effect] = system

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
          RabbitMqCommon.extractProperties(requestId, envelope.getRoutingKey, urlText, Option(consumerTag))
        logger.debug("Received AMQP request", requestProperties)

        // Process the request
        implicit val usingContext: RabbitMqServer.Context = RabbitMqCommon.context(amqpProperties)
        runEffect(
          genericHandler.processRequest(requestBody, requestId, None).either.map(_.fold(
            error => sendError(error, Option(amqpProperties.getReplyTo), requestProperties, requestId),
            result => {
              // Send the response
              val response = result.responseBody.getOrElse(Array[Byte]())
              sendResponse(response, Option(amqpProperties.getReplyTo), result.context, requestProperties, requestId)
            }
          ))
        )
        ()
      }
    }
    queues.foreach { queue =>
      consumer.getChannel.basicConsume(queue, true, consumer)
    }
    consumer
  }

  private def sendError(
    error: Throwable,
    replyTo: Option[String],
    requestProperties: => Map[String, String],
    requestId: String
  ): Unit = {
    logger.debug("Failed to process AMQP request", error, requestProperties)
    val message = Bytes.string.from(error.trace.mkString("\n")).unsafeArray
    sendResponse(message, replyTo, None, requestProperties, requestId)
  }

  private def sendResponse(
    message: Array[Byte],
    replyTo: Option[String],
    responseContext: Option[Context],
    requestProperties: => Map[String, String],
    requestId: String
  ): Unit = {
    // Log the response
    val actualReplyTo = replyTo.orElse(responseContext.flatMap { context =>
      context.replyTo.orElse(context.base.flatMap(base => Option(base.properties.getReplyTo)))
    })
    lazy val responseProperties = actualReplyTo.map { value =>
      requestProperties + (RabbitMqCommon.routingKeyProperty -> value)
    }.getOrElse(requestProperties - RabbitMqCommon.routingKeyProperty)
    logger.trace("Sending AMQP response", responseProperties)

    // Send the response
    Try {
      val routingKey = actualReplyTo.getOrElse {
        throw new IllegalArgumentException("Missing request header: reply-to")
      }
      val mediaType = genericHandler.protocol.codec.mediaType
      val amqpProperties = RabbitMqCommon.amqpProperties(responseContext, mediaType, routingKey, requestId, serverId)
      threadConsumer.get.getChannel.basicPublish(exchange, routingKey, true, false, amqpProperties, message)
      logger.debug("Sent AMQP response", responseProperties)
    }.onFailure(logger.error("Failed to send AMQP response", _, responseProperties)).get
  }

  private def createConnection(): Connection = RabbitMqCommon.connect(url, Seq.empty, serverId, connectionFactory)
}

object RabbitMqServer {

  /**
   * Asynchronous effect execution function type.
   *
   * @tparam Effect effect type
   */
  type Run[Effect[_]] = Effect[Any] => Unit

  /**
   * Creates a RabbitMQ server transport plugin with specified RPC request handler.
   *
   * Resulting function requires:
   * - effect execution function - executes specified effect asynchronously
   *
   * @param handler RPC request handler
   * @param url AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
   * @param queues names of non-durable exclusive queues to consume messages from
   * @param addresses broker hostnames and ports for reconnection attempts
   * @param connectionFactory AMQP broker connection factory
   * @tparam Effect effect type
   * @return creates a RabbitMQ server using supplied asynchronous effect execution function
   */
  def create[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, AmqpContext[BasicProperties]],
    url: URI,
    queues: Seq[String],
    addresses: Seq[Address] = Seq.empty,
    connectionFactory: ConnectionFactory = new ConnectionFactory
  ): Run[Effect] => RabbitMqServer[Effect] = (runEffect: Run[Effect]) =>
    RabbitMqServer(handler, url, queues, addresses, connectionFactory, runEffect)

  /** Request context type. */
  type Context = AmqpContext[RabbitMqContext]
}
