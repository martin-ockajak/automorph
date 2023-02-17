package automorph.transport.amqp.server

import automorph.Types
import automorph.log.{Logging, MessageLog}
import automorph.spi.EffectSystem
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.amqp.server.RabbitMqServer.Context
import automorph.transport.amqp.{AmqpContext, RabbitMqCommon, RabbitMqContext}
import automorph.util.Extensions.{ByteArrayOps, EffectOps, InputStreamOps, StringOps, ThrowableOps, TryOps}
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{Address, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope}
import java.net.URI
import scala.util.{Try, Using}
import scala.jdk.CollectionConverters.MapHasAsJava

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
 * @tparam Effect effect type
 */
final case class RabbitMqServer[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, AmqpContext[RabbitMqContext]],
  url: URI,
  queues: Seq[String],
  addresses: Seq[Address] = Seq.empty,
  connectionFactory: ConnectionFactory = new ConnectionFactory
) extends Logging with ServerMessageTransport[Effect] {

  private val exchange = RabbitMqCommon.defaultDirectExchange
  private lazy val connection = connect()
  private lazy val threadConsumer = RabbitMqCommon.threadLocalConsumer(connection, consumer)
  private val serverId = RabbitMqCommon.applicationId(getClass.getName)
  private val urlText = url.toString
  private val log = MessageLog(logger, RabbitMqCommon.protocol)
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, RabbitMqServer.Context]]
  implicit private val system: EffectSystem[Effect] = genericHandler.system
  start()

  override def close(): Effect[Unit] = system.evaluate(RabbitMqCommon.disconnect(connection))

  private def start(): Unit = {
    consumer(connection.createChannel())
    ()
  }

  private def consumer(channel: Channel): DefaultConsumer = {
    val consumer = new DefaultConsumer(channel) {

      override def handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        amqpProperties: BasicProperties,
        requestBody: Array[Byte]
      ): Unit = {
        // Log the request
        val requestId = Option(amqpProperties.getCorrelationId)
        lazy val requestProperties = RabbitMqCommon
          .messageProperties(requestId, envelope.getRoutingKey, urlText, Option(consumerTag))
        log.receivedRequest(requestProperties)
        Option(amqpProperties.getReplyTo).map { replyTo =>
          requestId.map { actualRequestId =>
            // Process the request
            val requestContext = RabbitMqCommon.messageContext(amqpProperties)
            genericHandler.processRequest(requestBody.toInputStream, requestContext, actualRequestId).either.map(_.fold(
              error => sendError(error, replyTo, requestProperties, actualRequestId),
              result => {
                // Send the response
                val response = result.responseBody.map(_.toArray).getOrElse(Array[Byte]())
                sendResponse(response, replyTo, result.context, requestProperties, actualRequestId)
              }
            )).runAsync
          }.getOrElse {
            logger.error(s"Missing ${log.defaultProtocol} request header: correlation-id", requestProperties)
          }
        }.getOrElse {
          logger.error(s"Missing ${log.defaultProtocol} request header: reply-to", requestProperties)
        }
      }
    }
    queues.foreach { queue =>
      consumer.getChannel.basicConsume(queue, true, consumer)
    }
    consumer
  }

  private def sendError(
    error: Throwable,
    replyTo: String,
    requestProperties: => Map[String, String],
    requestId: String
  ): Unit = {
    log.failedProcessRequest(error, requestProperties)
    val message = error.description.toInputStream.toArray
    sendResponse(message, replyTo, None, requestProperties, requestId)
  }

  private def sendResponse(
    message: Array[Byte],
    replyTo: String,
    responseContext: Option[Context],
    requestProperties: => Map[String, String],
    requestId: String
  ): Unit = {
    // Log the response
    val actualReplyTo = responseContext.flatMap { context =>
      context.replyTo.orElse(context.transport.flatMap {
        transport => Option(transport.properties.getReplyTo)
      })
    }.getOrElse(replyTo)
    lazy val responseProperties = requestProperties + (RabbitMqCommon.routingKeyProperty -> actualReplyTo)
    log.sendingResponse(responseProperties)

    // Send the response
    Try {
      val mediaType = genericHandler.protocol.codec.mediaType
      val amqpProperties = RabbitMqCommon
        .amqpProperties(responseContext, mediaType, actualReplyTo, requestId, serverId, useDefaultRequestId = true)
      threadConsumer.get.getChannel.basicPublish(exchange, actualReplyTo, true, false, amqpProperties, message)
      log.sentResponse(responseProperties)
    }.onFailure { error =>
      log.failedSendResponse(error, responseProperties)
    }.get
  }

  private def connect(): Connection = {
    val connection = RabbitMqCommon.connect(url, addresses, serverId, connectionFactory)
    RabbitMqCommon.declareExchange(exchange, connection)
    Using(connection.createChannel()) { channel =>
      queues.foreach { queue =>
        channel.queueDeclare(queue, false, false, false, Map.empty.asJava)
      }
    }
    connection
  }
}

object RabbitMqServer {

  /** Request context type. */
  type Context = AmqpContext[RabbitMqContext]
}
