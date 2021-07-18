package automorph.transport.amqp.server

import automorph.log.Logging
import automorph.spi.ServerMessageTransport
import automorph.transport.amqp.RabbitMqCommon.{applicationId, connect, defaultDirectExchange}
import com.rabbitmq.client.{AMQP, Address, BuiltinExchangeType, Channel, Connection, ConnectionFactory}
import java.io.IOException
import java.net.URL
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * RabbitMQ server transport plugin using AMQP as message transport protocol.
 *
 * @see [[https://www.rabbitmq.com/java-client.html Documentation]]
 * @see [[https://rabbitmq.github.io/rabbitmq-java-client/api/current/index.html API]]
 * @constructor Creates a RabbitMQ server transport plugin.
 * @param url AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
 * @param queueNames names of queues to consume messages from
 * @param handler RPC request handler
 * @param runEffectSync synchronous effect execution function
 * @tparam Effect effect type
 */
final case class RabbitMqServer[Effect[_]] (
  url: URL,
  queueNames: Seq[String],
  routingKey: String,
  exchangeName: String = defaultDirectExchange,
  exchangeType: BuiltinExchangeType = BuiltinExchangeType.DIRECT,
  addresses: Seq[Address] = Seq()
)(implicit executionContext: ExecutionContext)
  extends AutoCloseable with Logging with ServerMessageTransport {
  private lazy val connection = setupConnection()
  private lazy val threadChannel = ThreadLocal.withInitial(() => openChannel(connection))
  private val clientId = applicationId(getClass.getName)
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
//  * @param prefetchCount maximum number of concurrently processed requests, set to 0 for unlimited
//  * @param durable declare durable queues which will survive AMQP server restart
//  * @param exclusive declare exclusive queues accessible only to this connection
//  * @param autoDelete declare autodelete queues which are deleted once they are no longer in use
//  */

  override def close(): Unit = Await.result(connection, Duration.Inf).abort(AMQP.CONNECTION_FORCED, "Terminated")

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
