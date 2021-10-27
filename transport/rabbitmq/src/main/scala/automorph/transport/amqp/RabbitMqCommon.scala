package automorph.transport.amqp

import automorph.log.{LogProperties, Logging}
import automorph.transport.amqp.Amqp
import automorph.transport.amqp.client.RabbitMqClient.Context
import automorph.util.Extensions.TryOps
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{AMQP, Address, Channel, Connection, ConnectionFactory, DefaultConsumer}
import java.io.IOException
import java.net.{InetAddress, URI}
import java.util.Date
import scala.jdk.CollectionConverters.{MapHasAsJava, MapHasAsScala}
import scala.util.Try

/** Common RabbitMQ functionality. */
private[automorph] object RabbitMqCommon extends Logging {

  /** Default direct AMQP message exchange name. */
  val defaultDirectExchange: String = ""

  /** Routing key property. */
  val routingKeyProperty = "Routing Key"

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
    url: URI,
    addresses: Seq[Address],
    name: String,
    connectionFactory: ConnectionFactory
  ): Connection = {
    val urlText = url.toURL.toExternalForm
    connectionFactory.setUri(url)
    logger.debug(s"Connecting to RabbitMQ broker: $urlText")
    Try(if (addresses.nonEmpty) {
      connectionFactory.newConnection(addresses.toArray, name)
    } else {
      connectionFactory.newConnection(name)
    }).map { connection =>
      logger.info(s"Connected to RabbitMQ broker: $urlText")
      connection
    }.onFailure(logger.error(s"Failed to connect to RabbitMQ broker: $urlText", _)).get
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

  /**
   * @param requestId
   * @param mediaType
   * @param context
   * @return
   */

  /**
   * Create AMQP properties from message context.
   *
   * @param context message context
   * @param contentType MIME content type
   * @param defaultReplyTo address to reply to
   * @param defaultRequestId request identifier
   * @param defaultAppId application identifier
   * @return AMQP properties
   */
  def amqpProperties(
    context: Option[Context],
    contentType: String,
    defaultReplyTo: String,
    defaultRequestId: String,
    defaultAppId: String
  ): BasicProperties = {
    val amqp = context.getOrElse(Amqp())
    val baseProperties = amqp.base.map(_.properties).getOrElse(new BasicProperties())
    (new BasicProperties()).builder()
      .contentType(contentType)
      .replyTo(amqp.replyTo.orElse(Option(baseProperties.getReplyTo)).getOrElse(defaultReplyTo))
      .correlationId(amqp.correlationId.orElse(Option(baseProperties.getCorrelationId)).getOrElse(defaultRequestId))
      .contentEncoding(amqp.contentEncoding.orElse(Option(baseProperties.getContentEncoding)).orNull)
      .appId(amqp.appId.orElse(Option(baseProperties.getAppId)).getOrElse(defaultAppId))
      .headers((amqp.headers ++ baseProperties.getHeaders.asScala).asJava)
      .deliveryMode(amqp.deliveryMode.map(new Integer(_)).orElse(Option(baseProperties.getDeliveryMode)).orNull)
      .priority(amqp.priority.map(new Integer(_)).orElse(Option(baseProperties.getPriority)).orNull)
      .expiration(amqp.expiration.orElse(Option(baseProperties.getExpiration)).orNull)
      .messageId(amqp.messageId.orElse(Option(baseProperties.getMessageId)).orNull)
      .timestamp(amqp.timestamp.map(Date.from).orElse(Option(baseProperties.getTimestamp)).orNull)
      .`type`(amqp.`type`.orElse(Option(baseProperties.getType)).orNull)
      .userId(amqp.userId.orElse(Option(baseProperties.getUserId)).orNull)
      .build
  }

  /**
   * Create message context from AMQP properties.
   *
   * @param properties message properties
   * @return message context
   */
  def context(properties: BasicProperties): Amqp[RabbitMqContext] =
    Amqp(
      contentType = Option(properties.getContentType),
      contentEncoding = Option(properties.getContentEncoding),
      headers = Option(properties.getHeaders).map(headers => Map.from(headers.asScala)).getOrElse(Map.empty),
      deliveryMode = Option(properties.getDeliveryMode.toInt),
      priority = Option(properties.getPriority.toInt),
      correlationId = Option(properties.getCorrelationId),
      replyTo = Option(properties.getReplyTo),
      expiration = Option(properties.getExpiration),
      messageId = Option(properties.getMessageId),
      timestamp = Option(properties.getTimestamp).map(_.toInstant),
      `type` = Option(properties.getType),
      userId = Option(properties.getUserId),
      appId = Option(properties.getAppId),
      base = Some(RabbitMqContext(properties))
    )

  /**
   * Extract message properties from message metadata.
   *
   * @param requestId request correlation identifier
   * @param routingKey routing key
   * @param url AMQP broker URL
   * @param consumerTag consumer tag
   * @return message properties
   */
  def extractProperties(
    requestId: String,
    routingKey: String,
    url: String,
    consumerTag: Option[String]
  ): Map[String, String] = Map(
    LogProperties.requestId -> requestId,
    routingKeyProperty -> routingKey,
    "URL" -> url
  ) ++ consumerTag.map("Consumer Tag" -> _)
}

final case class RabbitMqContext(properties: BasicProperties)

object RabbitMqContext {

  /** Implicit default context value. */
  implicit val default: Amqp[RabbitMqContext] = Amqp()
}
