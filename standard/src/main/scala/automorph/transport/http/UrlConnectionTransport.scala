package automorph.transport.http

import java.net.{HttpURLConnection, URL}
import automorph.backend.IdentityBackend.Identity
import automorph.spi.ClientMessageTransport
import automorph.transport.http.UrlConnectionTransport.RequestProperties
import scala.collection.immutable.ArraySeq
import automorph.handler.Bytes.inputStreamBytes
import scala.util.{Try, Using}

/**
 * URL connection transport plugin using HTTP as message transport protocol.
 *
 * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html API]]
 * @constructor Creates an URL connection transport plugin using HTTP as message transport protocol.
 * @param url HTTP endpoint URL
 * @param method HTTP method
 */
final case class UrlConnectionTransport(
  url: URL,
  method: String
) extends ClientMessageTransport[Identity, RequestProperties] {

  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): Identity[ArraySeq.ofByte] = {
    val connection = send(request, mediaType, context)
    Using.resource(connection.getInputStream)(inputStreamBytes.from)
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[RequestProperties]): Identity[Unit] = {
    send(request, mediaType, context)
    ()
  }

  override def defaultContext: RequestProperties = RequestProperties.defaultContext.copy(method = Some(method))

  private def send(request: ArraySeq.ofByte, mediaType: String, context: Option[RequestProperties]): HttpURLConnection = {
    val connection = connect()
    val outputStream = connection.getOutputStream
    context.foreach(setProperties(connection, request, mediaType, _))
    val trySend = Try(outputStream.write(request.unsafeArray))
    context.foreach(clearProperties(connection, _))
    outputStream.close()
    trySend.get
    connection
  }

  private def setProperties(
    connection: HttpURLConnection,
    request: ArraySeq.ofByte,
    mediaType: String,
    context: RequestProperties
  ): Unit = {
    // Validate HTTP request properties
    val httpMethod = context.method.getOrElse(method)
    require(httpMethods.contains(httpMethod), s"Invalid HTTP method: $httpMethod")

    // Set HTTP request context
    connection.setRequestProperty(contentLengthHeader, request.size.toString)
    connection.setRequestProperty(contentTypeHeader, mediaType)
    connection.setRequestProperty(acceptHeader, mediaType)
    connection.setRequestMethod(httpMethod)
    connection.setConnectTimeout(context.connectTimeout)
    connection.setReadTimeout(context.readTimeout)
    context.headers.foreach { case (key, value) =>
      connection.setRequestProperty(key, value)
    }
  }

  private def clearProperties(connection: HttpURLConnection, context: RequestProperties): Unit =
    context.headers.foreach { case (key, _) =>
      connection.setRequestProperty(key, null)
    }

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

case object UrlConnectionTransport {

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
