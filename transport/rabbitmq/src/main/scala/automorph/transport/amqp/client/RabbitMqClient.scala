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
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

/**
 * RabbitMQ client transport plugin using AMQP as message transport protocol.
 *
 * @see [[https://www.rabbitmq.com/java-client.html Documentation]]
 * @see [[https://rabbitmq.github.io/rabbitmq-java-client/api/current/index.html API]]
 * @constructor Creates a RabbitMQ client transport plugin.
 * @param url AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
 * @param routingKey AMQP routing key (typically a queue name)
 * @param exchangeName AMQP message exchange name
 * @param exchangeType AMQP message exchange type (DIRECT, FANOUT, HEADERS, TOPIC)
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
  extends AutoCloseable with Logging with ClientMessageTransport[Future, RequestProperties] {

  private val directReplyToQueue = "amq.rabbitmq.reply-to"
  private lazy val connection = setupConnection()
  private lazy val threadConsumer = ThreadLocal.withInitial(() => createConsumer(connection))
  private val urlText = url.toExternalForm
  private val clientId = applicationId(getClass.getName)
  private val callResults = TrieMap[String, Promise[ArraySeq.ofByte]]()

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

  override def close(): Unit = Await.result(connection, Duration.Inf).abort(AMQP.CONNECTION_FORCED, "Terminated")

  private def send(
    request: ArraySeq.ofByte,
    properties: BasicProperties,
    result: Option[Promise[ArraySeq.ofByte]]
  ): Future[Unit] =
    threadConsumer.get.flatMap { consumer =>
      logger.trace(
        "Sending AMQP request",
        Map(
          "URL" -> urlText,
          "Routing key" -> routingKey,
          "Correlation ID" -> properties.getCorrelationId,
          "Size" -> request.length
        )
      )
      result.foreach(callResults.put(properties.getCorrelationId, _))
      Future(
        consumer.channel.basicPublish(exchangeName, routingKey, true, false, properties, request.unsafeArray)
      ).transform(
        _ =>
          logger.debug(
            "Sent AMQP request",
            Map(
              "URL" -> urlText,
              "Routing key" -> routingKey,
              "Correlation ID" -> properties.getCorrelationId,
              "Size" -> request.length
            )
          ),
        error => {
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
        }
      )
    }

  private def setupProperties(mediaType: String, context: Option[RequestProperties]): BasicProperties =
    context.getOrElse(defaultContext).basic.builder().replyTo(directReplyToQueue).correlationId(MessageId.next)
      .contentType(mediaType).appId(clientId).build

  private def createConsumer(connection: Future[Connection]): Future[ResponseConsumer] =
    connection.map(_.createChannel()).map { channel =>
      val consumer = ResponseConsumer(Option(channel).getOrElse {
        throw new IOException("No AMQP connection channel available")
      })
      consumer.channel.basicConsume(directReplyToQueue, true, consumer)
      consumer
    }

  private def setupConnection(): Future[Connection] = {
    val connectionFactory = new ConnectionFactory
    Future(connect(url, Seq(), connectionFactory, clientId)).flatMap { connection =>
      if (exchangeName != defaultDirectExchange) {
        threadConsumer.get().map { consumer =>
          consumer.channel.exchangeDeclare(exchangeName, exchangeType, false)
          connection
        }
      } else {
        Future.successful(connection)
      }
    }
  }

  final private case class ResponseConsumer(channel: Channel) extends DefaultConsumer(channel) {

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
}

case object RabbitMqClient {

  case class RequestProperties(
    basic: BasicProperties
  )

  case object RequestProperties {
    implicit val defaultContext: RequestProperties = RequestProperties(new BasicProperties)
  }
}
