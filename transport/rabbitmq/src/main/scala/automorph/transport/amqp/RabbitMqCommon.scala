package automorph.transport.amqp

import automorph.log.Logging
import automorph.util.Extensions.TryOps
import com.rabbitmq.client.{Address, Connection, ConnectionFactory}
import java.net.URL
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
   * @param connectionName connection name
   * @return AMQP broker connection, virtual host
   */
  def connect(
    url: URL,
    addresses: Seq[Address],
    connectionFactory: ConnectionFactory,
    connectionName: String
  ): Connection = {
    val urlText = url.toExternalForm
    connectionFactory.setUri(url.toURI)
    logger.debug(s"Connecting to RabbitMQ broker: $urlText")
    Try(if (addresses.nonEmpty) {
      connectionFactory.newConnection(addresses.toArray, connectionName)
    } else {
      connectionFactory.newConnection(connectionName)
    }).map { connection =>
      logger.info(s"Connected to RabbitMQ broker: $urlText")
      connection
    }.mapFailure { error =>
      logger.error(s"Failed to connect to RabbitMQ broker: $urlText", error)
      error
    }.get
  }
}
