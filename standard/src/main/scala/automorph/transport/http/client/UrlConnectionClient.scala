package automorph.transport.http.client

import automorph.log.Logging
import automorph.spi.ClientMessageTransport
import automorph.transport.http.client.UrlConnectionClient.Context
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.HttpProperties
import automorph.util.Bytes
import automorph.util.Extensions.TryOps
import java.net.{HttpURLConnection, URI}
import scala.collection.immutable.ArraySeq
import scala.util.{Try, Using}

/**
 * URL connection client transport plugin using HTTP as message transport protocol.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html API]]
 * @constructor Creates an URL connection client transport plugin.
 * @param url HTTP endpoint URL
 * @param method HTTP method
 */
final case class UrlConnectionClient(
  url: URI,
  method: String
) extends ClientMessageTransport[Identity, Context] with Logging {

  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
  require(httpMethods.contains(method), s"Invalid HTTP method: $method")

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): Identity[ArraySeq.ofByte] = {
    val connection = send(request, mediaType, context)
    logger.trace("Receiving HTTP response", Map("URL" -> url))
    Try(Using.resource(connection.getInputStream)(Bytes.inputStream.from)).pureFold(
      error => {
        logger.error("Failed to receive HTTP response", error, Map("URL" -> url))
        throw error
      },
      response => {
        logger.debug("Received HTTP response", Map("URL" -> url, "Status" -> connection.getResponseCode, "Size" -> response.length))
        response
      }
    )
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Identity[Unit] = {
    send(request, mediaType, context)
    ()
  }

  override def defaultContext: Context = UrlConnectionClient.defaultContext.copy(method = Some(method))

  private def send(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): HttpURLConnection = {
    logger.trace("Sending HTTP request", Map("URL" -> url, "Size" -> request.length))
    val connection = connect()
    val outputStream = connection.getOutputStream
    val httpMethod = setProperties(connection, request, mediaType, context)
    val trySend = Using(outputStream)(_.write(request.unsafeArray))
    clearProperties(connection, context)
    trySend.pureFold(
      error => {
        logger.error(
          "Failed to send HTTP request",
          error,
          Map("URL" -> url, "Method" -> httpMethod, "Size" -> request.length)
        )
        throw error
      },
      _ => logger.debug("Sent HTTP request", Map("URL" -> url, "Method" -> httpMethod, "Size" -> request.length))
    )
    connection
  }

  private def setProperties(
    connection: HttpURLConnection,
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): String = {
    val properties = context.getOrElse(defaultContext)
    val httpMethod = properties.method.getOrElse(method)
    require(httpMethods.contains(httpMethod), s"Invalid HTTP method: $httpMethod")
    connection.setRequestMethod(httpMethod)
    connection.setConnectTimeout(properties.readTimeout.toMillis.toInt)
    connection.setReadTimeout(properties.readTimeout.toMillis.toInt)
    connection.setRequestProperty(contentLengthHeader, request.size.toString)
    connection.setRequestProperty(contentTypeHeader, mediaType)
    connection.setRequestProperty(acceptHeader, mediaType)
    properties.path.getOrElse(url.getPath)
    properties.headers.foreach { case (key, value) =>
      connection.setRequestProperty(key, value)
    }
    httpMethod
  }

  private def clearProperties(connection: HttpURLConnection, context: Option[Context]): Unit =
    context.foreach(_.headers.foreach { case (key, _) =>
      connection.setRequestProperty(key, null)
    })

  /**
   * Open new HTTP connections.
   *
   * @return HTTP connection
   */
  private def connect(): HttpURLConnection = {
    val connection = url.toURL.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection
  }
}

case object UrlConnectionClient {

  /** Request context type. */
  type Context = HttpProperties[HttpURLConnection]

  implicit val defaultContext: Context = HttpProperties()
}
