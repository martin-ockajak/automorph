package automorph.transport.http.client

import automorph.handler.Bytes.inputStreamBytes
import automorph.log.Logging
import automorph.spi.ClientMessageTransport
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.client.UrlConnectionClient.RequestProperties
import automorph.util.Extensions.TryOps
import java.net.{HttpURLConnection, URL}
import scala.collection.immutable.ArraySeq
import scala.util.{Try, Using}

/**
 * URL connection client transport plugin using HTTP as message transport protocol.
 *
 * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html API]]
 * @constructor Creates an URL connection client transport plugin.
 * @param url HTTP endpoint URL
 * @param method HTTP method
 */
final case class UrlConnectionClient(
  url: URL,
  method: String
) extends ClientMessageTransport[Identity, RequestProperties] with Logging {

  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
  private val urlText = url.toExternalForm

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): Identity[ArraySeq.ofByte] = {
    val connection = send(request, mediaType, context)
    logger.trace("Receiving HTTP response", Map("URL" -> urlText))
    Try(Using.resource(connection.getInputStream)(inputStreamBytes.from)).pureFold(
      error => {
        logger.error("Failed to receive HTTP response", error, Map("URL" -> urlText))
        throw error
      },
      response => {
        logger.debug("Received HTTP response", Map("URL" -> urlText, "Status" -> connection.getResponseCode, "Size" -> response.length))
        response
      }
    )
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[RequestProperties]): Identity[Unit] = {
    send(request, mediaType, context)
    ()
  }

  override def defaultContext: RequestProperties = RequestProperties.defaultContext.copy(method = Some(method))

  private def send(request: ArraySeq.ofByte, mediaType: String, context: Option[RequestProperties]): HttpURLConnection = {
    logger.trace("Sending HTTP request", Map("URL" -> urlText, "Size" -> request.length))
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
          Map("URL" -> urlText, "Method" -> httpMethod, "Size" -> request.length)
        )
        throw error
      },
      _ => logger.debug("Sent HTTP request", Map("URL" -> urlText, "Method" -> httpMethod, "Size" -> request.length))
    )
    connection
  }

  private def setProperties(
    connection: HttpURLConnection,
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): String = {
    // Validate HTTP request properties
    val requestProperties = context.getOrElse(defaultContext)
    val httpMethod = requestProperties.method.getOrElse(method)
    require(httpMethods.contains(httpMethod), s"Invalid HTTP method: $httpMethod")

    // Set HTTP request requestProperties
    requestProperties.headers.foreach { case (key, value) =>
      connection.setRequestProperty(key, value)
    }
    connection.setRequestProperty(contentLengthHeader, request.size.toString)
    connection.setRequestProperty(contentTypeHeader, mediaType)
    connection.setRequestProperty(acceptHeader, mediaType)
    connection.setRequestMethod(httpMethod)
    connection.setConnectTimeout(requestProperties.connectTimeout)
    connection.setReadTimeout(requestProperties.readTimeout)
    httpMethod
  }

  private def clearProperties(connection: HttpURLConnection, context: Option[RequestProperties]): Unit =
    context.foreach(_.headers.foreach { case (key, _) =>
      connection.setRequestProperty(key, null)
    })

  /**
   * Open new HTTP connections.
   *
   * @return HTTP connection
   */
  private def connect(): HttpURLConnection = {
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection
  }
}

case object UrlConnectionClient {

  /** Request context type. */
  type Context = RequestProperties

  /**
   * HTTP request context.
   *
   * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html Documentation]]
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param headers HTTP headers
   * @param followRedirects automatically follow HTTP redirects
   * @param connectTimeout connection timeout (milliseconds)
   * @param readTimeout read timeout (milliseconds)
   */
  case class RequestProperties(
    method: Option[String] = None,
    headers: Map[String, String] = Map.empty,
    followRedirects: Boolean = true,
    connectTimeout: Int = 30000,
    readTimeout: Int = 30000
  )

  case object RequestProperties {
    implicit val defaultContext: RequestProperties = RequestProperties()
  }
}
