package automorph.transport.amqp.server

import automorph.Handler
import automorph.log.Logging
import automorph.spi.ServerMessageTransport
import automorph.transport.amqp.RabbitMqCommon
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{AMQP, Address, BuiltinExchangeType, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope}
import java.io.IOException
import java.net.URL
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

/**
 * RabbitMQ server transport plugin using AMQP as message transport protocol.
 *
 * The server interprets incoming AMQP message body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as outgoing AMQP message body.
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
  handler: Handler.AnyFormat[Effect, BasicProperties],
  runEffect: Effect[Any] => Any,
  url: URL,
  queues: Seq[String],
  addresses: Seq[Address] = Seq(),
  connectionFactory: ConnectionFactory = new ConnectionFactory
)(implicit executionContext: ExecutionContext)
  extends AutoCloseable with Logging with ServerMessageTransport {

  private lazy val connection = createConnection()
  private val clientId = RabbitMqCommon.applicationId(getClass.getName)
//class AmqpJsonRpcServer(url: String, requestTimeout: FiniteDuration, queueNames: Traversable[String], prefetchCount: Int = 0,
//  durable: Boolean = false, exclusive: Boolean = false, autoDelete: Boolean = false, applicationId: String = InetAddress.getLocalHost.getHostName)
//  setupQueueConsumers()
//
///**
//  * JSON-RPC over AMQP server based on RabbitMQ client.
//  * Declares specified queues.
//  * Interprets AMQP messages as JSON-RPC requests.
//  * Publishes responses to default exchange and uses 'reply-to' request property as routing key.
//  * @param url AMQP server URL in following format: amqp[s]://[username:password@]host[:port][/virtual host]
//  */

  override def close(): Unit = connection.abort(AMQP.CONNECTION_FORCED, "Terminated")

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
            "Routing key" -> envelope.getRoutingKey,
            "Correlation ID" -> properties.getCorrelationId,
            "Size" -> body.length
          )
        )
//        callResults.get(properties.getCorrelationId).foreach { promise =>
//          promise.success(new ArraySeq.ofByte(body))
//        }
      }
    }
    queues.foreach { queue =>
      consumer.getChannel.basicConsume(queue, true, consumer)
    }
    consumer
  }

  private def createConnection(): Connection = RabbitMqCommon.connect(url, Seq(), clientId, connectionFactory)

//  private def setupQueueConsumers(): Unit = {
//    logger.info(s"Consuming messages from queues: ${queueNames.mkString(", ")}")
//    for (queueName <- queueNames) yield {
//      // create queue and allow consuming only one message without acknowledgement
//      val channel = connection.createChannel()
//      channel.queueDeclare(queueName, durable, exclusive, autoDelete, null)
//      channel.basicQos(prefetchCount)
//
//      // consume messages from the queue
//      channel.basicConsume(queueName, false, InetAddress.getLocalHost.getHostName,
//        new QueueConsumer(channel, virtualHost, applicationId, this))
//    }
//  }
}
