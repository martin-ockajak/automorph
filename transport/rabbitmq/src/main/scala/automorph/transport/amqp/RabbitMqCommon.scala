package automorph.transport.amqp

import automorph.log.Logging
import automorph.util.Extensions.TryOps
import com.rabbitmq.client.{AMQP, Address, Channel, Connection, ConnectionFactory, DefaultConsumer}
import java.io.IOException
import java.net.{InetAddress, URL}
import scala.util.Try

/** Common RabbitMQ functionality. */
private[automorph] object RabbitMqCommon extends Logging {

  /** Default direct AMQP message exchange name. */
  val defaultDirectExchange: String = ""

  /**
   * Initialize AMQP broker connection.
   *
   * @param url AMQP broker URL (amqp[s]://[username:password@]host[:port][/virtual_host])
   * @param addresses broker hostnames and ports for reconnection attempts
   * @param connectionFactory connection factory
   * @param name connection name
   * @return AMQP broker connection
   */
  def connect(
    url: URL,
    addresses: Seq[Address],
    name: String,
    connectionFactory: ConnectionFactory
  ): Connection = {
    val urlText = url.toExternalForm
    connectionFactory.setUri(url.toURI)
    logger.debug(s"Connecting to RabbitMQ broker: $urlText")
    Try(if (addresses.nonEmpty) {
      connectionFactory.newConnection(addresses.toArray, name)
    } else {
      connectionFactory.newConnection(name)
    }).map { connection =>
      logger.info(s"Connected to RabbitMQ broker: $urlText")
      connection
    }.mapFailure { error =>
      logger.error(s"Failed to connect to RabbitMQ broker: $urlText", error)
      error
    }.get
  }

  /**
   * Close AMQP broker connection.
   *
   * @param connection AMQP broker connection
   */
  def disconnect(connection: Connection): Unit = connection.abort(AMQP.CONNECTION_FORCED, "Terminated")

  /**
   * Returns application identifier combining the local host name with specified application name.
   *
   * @param applicationName application name
   * @return application identifier
   */
  def applicationId(applicationName: String): String = s"${InetAddress.getLocalHost.getHostName}/$applicationName"

  /**
   * Creates thread-local AMQP message consumer for specified connection,
   *
   * @param connection AMQP broker connection
   * @param createConsumer AMQP message consumer creation function
   * @tparam T AMQP message consumer type
   * @return thread-local AMQP message consumer
   */
  def threadLocalConsumer[T <: DefaultConsumer](connection: Connection, createConsumer: Channel => T): ThreadLocal[T] =
    ThreadLocal.withInitial { () =>
      val channel = connection.createChannel()
      createConsumer(Option(channel).getOrElse {
        throw new IOException("No AMQP connection channel available")
      })
    }
}
