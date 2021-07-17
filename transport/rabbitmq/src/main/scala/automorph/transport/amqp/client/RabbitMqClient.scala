package automorph.transport.amqp.client

import automorph.log.Logging
import automorph.spi.ClientMessageTransport
import automorph.transport.amqp.RabbitMqCommon.{applicationId, connect, defaultDirectExchange}
import automorph.transport.amqp.client.RabbitMqClient.RequestProperties
import automorph.util.MessageId
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{AMQP, Address, BuiltinExchangeType, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope}
import java.io.IOException
import java.net.URL
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Random, Success, Try}

/**
 * RabbitMQ client transport plugin using AMQP as message transport protocol.
 *
 * @param url AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
 * @param routingKey AMQP routing key (typically a queue name)
 * @param exchangeName AMQP message exchange name
 * @param exchangeName AMQP message exchange type (DIRECT, FANOUT, HEADERS, TOPIC)
 * @param addresses broker hostnames and ports for reconnection attempts
 * @param executionContext execution context
 */
final case class RabbitMqClient(
  url: URL,
  routingKey: String,
  exchangeName: String = defaultDirectExchange,
  exchangeType: BuiltinExchangeType = BuiltinExchangeType.DIRECT,
  addresses: Seq[Address] = Seq()
)(implicit executionContext: ExecutionContext)
  extends ClientMessageTransport[Future, RequestProperties] with AutoCloseable with Logging {

  private val directReplyToQueue = "amq.rabbitmq.reply-to"
  private lazy val connection = setupConnection()
  private lazy val threadChannel = ThreadLocal.withInitial(() => openChannel(connection))
  private val urlText = url.toExternalForm
  private val clientId = applicationId(getClass.getName)

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): Future[ArraySeq.ofByte] = {
    val properties = setupProperties(mediaType, context)
    val promise = Promise[ArraySeq.ofByte]()
    val consumer = (channel: Channel) => ResponseConsumer(channel, promise)
    send(request, properties, Some(consumer)).flatMap(_ => promise.future)
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[RequestProperties]): Future[Unit] = {
    val properties = setupProperties(mediaType, context)
    send(request, properties, None)
  }

  override def defaultContext: RequestProperties = RequestProperties.defaultContext

  override def close(): Unit = Await.result(connection, Duration.Inf).abort(AMQP.CONNECTION_FORCED, "Terminated")

  private def send(
    request: ArraySeq.ofByte,
    properties: BasicProperties,
    consumer: Option[Channel => ResponseConsumer]
  ): Future[Unit] =
    threadChannel.get.flatMap { channel =>
      consumer.foreach(actualConsumer => channel.basicConsume(directReplyToQueue, true, actualConsumer(channel)))
      logger.trace("Sending AMQP request", Map("URL" -> urlText, "Routing key" -> routingKey, "Size" -> request.length))
      Future(channel.basicPublish(exchangeName, routingKey, true, false, properties, request.unsafeArray)).transform(
        _ => {
          logger.debug("Sent AMQP request", Map("URL" -> urlText, "Routing key" -> routingKey, "Size" -> request.length))
          ()
        },
        error => {
          logger.error(
            "Failed to send AMQP request",
            error,
            Map("URL" -> urlText, "Routing key" -> routingKey, "Size" -> request.length)
          )
          error
        }
      )
    }

  private def setupProperties(mediaType: String, context: Option[RequestProperties]): BasicProperties =
    context.getOrElse(defaultContext).basic.builder().replyTo(directReplyToQueue).correlationId(MessageId.next)
      .contentType(mediaType).appId(clientId).build

  private def openChannel(connection: Future[Connection]): Future[Channel] =
    connection.map(_.createChannel()).map { channel =>
      Option(channel).getOrElse {
        throw new IOException("No AMQP connection channel available")
      }
    }

  private def setupConnection(): Future[Connection] = {
    val connectionFactory = new ConnectionFactory
    Future(connect(url, Seq(), connectionFactory, clientId)).flatMap { connection =>
      if (exchangeName != defaultDirectExchange) {
        threadChannel.get().map { channel =>
          channel.exchangeDeclare(exchangeName, exchangeType, false)
          connection
        }
      } else {
        Future.successful(connection)
      }
    }
  }

  final private case class ResponseConsumer(channel: Channel, promise: Promise[ArraySeq.ofByte])
    extends DefaultConsumer(channel) {

    override def handleDelivery(
      consumerTag: String,
      envelope: Envelope,
      properties: BasicProperties,
      body: Array[Byte]
    ): Unit = {
      logger.debug(
        "Received AMQP response",
        Map("URL" -> urlText, "Routing key" -> routingKey, "Size" -> body.length)
      )
      promise.success(ArraySeq.ofByte(body))
    }
  }
}

case object RabbitMqClient {

  case class RequestProperties(
    basic: BasicProperties
  )

  case object RequestProperties {
    implicit val defaultContext: RequestProperties = RequestProperties(BasicProperties())
  }
}
