package automorph.transport.amqp.client

import automorph.log.Logging
import automorph.spi.ClientMessageTransport
import automorph.transport.amqp.RabbitMqCommon.{connect, defaultDirectExchange}
import automorph.transport.amqp.client.RabbitMqClient.RequestProperties
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{AMQP, Address, Connection, ConnectionFactory}
import java.net.{InetAddress, URL}
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try, Using}

/**
 * RabbitMQ client transport plugin using AMQP as message transport protocol.
 *
 * @param url AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
 * @param routingKey AMQP routing key (typically a queue name)
 * @param exchange AMQP message exchange name
 * @param applicationId AMQP message application identifier
 * @param addresses broker hostnames and ports for reconnection attempts
 * @tparam Effect effect type
 */
final case class RabbitMqClient[Effect[_]](
  url: URL,
  routingKey: String,
  exchange: String = defaultDirectExchange,
  addresses: Seq[Address] = Seq(),
  applicationId: String = InetAddress.getLocalHost.getHostName
)(implicit executionContext: ExecutionContext)
  extends ClientMessageTransport[Effect, RequestProperties] with AutoCloseable with Logging {

  private val directExchange = "direct"
  private val directReplyToQueue = "amq.rabbitmq.reply-to"
  private lazy val connection = setupConnection()
  private val timeout = 0

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
//
//  override def sendNotifyRequest(requestMessage: Array[Byte]): Future[Unit] =
//    Future {
//      val channel = connection.createChannel()
//      try {
//        channel.basicPublish(exchange, routingKey, true, null, requestMessage)
//      } finally {
//        channel.close()
//      }
//    }

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): Effect[ArraySeq.ofByte] = ???

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[RequestProperties]): Effect[Unit] =
    ???

  override def defaultContext: RequestProperties = RequestProperties.defaultContext

  private def setupConnection(): Try[Connection] = {
    val connectionFactory = new ConnectionFactory
    connect(url, Seq(), connectionFactory, getClass.getName).flatMap { connection =>
      if (exchange != defaultDirectExchange) {
        Using(connection.createChannel()) { channel =>
          channel.exchangeDeclare(exchange, directExchange, false)
          connection
        }
      } else {
        Success(connection)
      }
    }
  }

  override def close(): Unit = connection.foreach(_.abort(AMQP.CONNECTION_FORCED, "Terminated", timeout))
}

case object RabbitMqClient {

  case class RequestProperties(
  )

  case object RequestProperties {
    implicit val defaultContext: RequestProperties = RequestProperties()
  }
}
