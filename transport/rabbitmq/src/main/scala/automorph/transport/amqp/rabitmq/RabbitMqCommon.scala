package automorph.transport.amqp

import automorph.log.Logging
import automorph.util.Extensions.TryOps
import com.rabbitmq.client.{Address, Connection, ConnectionFactory}
import java.net.URL
import scala.util.Try

/**
  * Common RabbitMQ functionality.
  */
object RabbitMqCommon extends Logging {
  /**
    * Initialize AMQP server connection.
    *
    * @param url AMQP server URL
   *  @param addresses broker hostnames and ports for connection attempts
   *  @param clientName client name
    * @return AMQP server connection, virtual host
    */
  def setupConnection(url: URL, addresses: Seq[Address], clientName: String, connectionFactory: ConnectionFactory): Try[(Connection, String)] = {
    // initialize connection
    val urlText = url.toExternalForm
    logger.debug(s"Connecting to RabbitMQ broker: $urlText")
    connectionFactory.setUri(url.toURI)
    Try {
      val connection = if (addresses.nonEmpty) {
        connectionFactory.newConnection(addresses.toArray, clientName)
      } else {
        connectionFactory.newConnection(clientName)
      }
      logger.info(s"Connected to RabbitMQ broker: $urlText")
      (connection, connectionFactory.getVirtualHost)
    }.mapFailure { error =>
      logger.error( s"Failed to connect to RabbitMQ broker: $urlText", error)
      error
    }
  }
}
