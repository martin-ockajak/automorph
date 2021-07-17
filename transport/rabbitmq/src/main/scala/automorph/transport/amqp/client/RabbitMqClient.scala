package automorph.transport.amqp.client

import automorph.log.Logging
import automorph.spi.ClientMessageTransport
import automorph.transport.amqp.RabbitMqCommon.{connect, defaultDirectExchange}
import automorph.transport.amqp.client.RabbitMqClient.RequestProperties
import automorph.util.MessageId
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{AMQP, Address, BuiltinExchangeType, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope}
import java.io.IOException
import java.net.{InetAddress, URL}
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
  private val timeout = 0

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): Future[ArraySeq.ofByte] = {
    val properties = setupProperties(mediaType, context)
    send(request, properties)
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[RequestProperties]): Future[Unit] = {
    val properties = setupProperties(mediaType, context)
    send(request, properties).map(_ => ())
  }

  private def send(request: ArraySeq.ofByte, properties: BasicProperties): Future[ArraySeq.ofByte] =
    threadChannel.get.flatMap { channel =>
      val promise = Promise[ArraySeq.ofByte]()
      val consumer = new DefaultConsumer(channel) {

        override def handleDelivery(
          consumerTag: String,
          envelope: Envelope,
          properties: BasicProperties,
          body: Array[Byte]
        ): Unit = promise.success(ArraySeq.ofByte(body))
      }
      channel.basicConsume(directReplyToQueue, true, consumer)
      channel.basicPublish(exchangeName, routingKey, properties, request.unsafeArray)
      promise.future
    }

  private def setupProperties(mediaType: String, context: Option[RequestProperties]): BasicProperties = {
    val properties = context.getOrElse(defaultContext).basic
    properties.builder().contentType(mediaType).correlationId(MessageId.next).build
  }

  //  override def sendCallRequest(requestMessage: Array[Byte]): Future[Array[Byte]] =
//    Future {
//      val channel = connection.createChannel()
//      try {
//        val consumer = new QueueingConsumer(channel)
//        channel.basicConsume(directReplyToQueue, true, consumer)
//        val properties = new BasicProperties().builder().replyTo(directReplyToQueue)
//        channel.basicPublish(exchange, routingKey, true, properties, requestMessage)
//        val response = consumer.nextDelivery(timeout.toLong)
//        val envelope = response.getEnvelope
//        logger.debug(
//          s"Received response: Queue = ${envelope.getRoutingKey}, Delivery tag = ${envelope.getDeliveryTag}, Application = ${response.getProperties.getAppId}"
//        )
//        response.getBody
////        val rpcClient = new RpcClient(channel, exchange, routingKey, timeout)
////        rpcClient.checkConsumer()
////        rpcClient.primitiveCall(properties, requestMessage)
//      } finally {
//        Try(channel.close())
//      }
//    }

  override def defaultContext: RequestProperties = RequestProperties.defaultContext

  private def openChannel(connection: Future[Connection]): Future[Channel] =
    connection.map(_.createChannel()).map { channel =>
      Option(channel).getOrElse {
        throw new IOException("No AMQP connection channel available")
      }
    }

  private def setupConnection(): Future[Connection] = {
    val connectionFactory = new ConnectionFactory
    val clientName = s"${InetAddress.getLocalHost.getHostName}/${getClass.getName}"
    Future(connect(url, Seq(), connectionFactory, clientName)).flatMap { connection =>
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

  override def close(): Unit =
    Await.result(connection.map(_.abort(AMQP.CONNECTION_FORCED, "Terminated", timeout)), Duration.Inf)
}

case object RabbitMqClient {

  case class RequestProperties(
    basic: BasicProperties
  )

  case object RequestProperties {
    implicit val defaultContext: RequestProperties = RequestProperties(BasicProperties())
  }
}
