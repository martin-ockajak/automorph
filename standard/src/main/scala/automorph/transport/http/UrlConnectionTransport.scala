package automorph.transport.http

import java.io.ByteArrayOutputStream
import java.net.{HttpURLConnection, URL}
import automorph.backend.IdentityBackend.Identity
import automorph.spi.{Backend, Transport}
import automorph.transport.http.UrlConnectionTransport.HttpContext
import scala.collection.immutable.ArraySeq
import scala.util.{Try, Using}

/**
 * URL connection HTTP transport.
 *
 * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html API]]
 * @param url HTTP endpoint URL
 * @param contentType HTTP request Content-Type
 * @param bufferSize input stream reading buffer size
 * @tparam Effect effect type
 */
final case class UrlConnectionTransport(
  url: URL,
  contentType: String,
  bufferSize: Int = 4096
) extends Transport[Identity, HttpContext] {

  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
  private val maxReadIterations = 1024 * 1024

  override def call(request: ArraySeq.ofByte, context: Option[HttpContext]): Identity[ArraySeq.ofByte] = {
    val connection = send(request, context)
    Using.resource(connection.getInputStream) { inputStream =>
      val outputStream = new ByteArrayOutputStream()
      val buffer = Array.ofDim[Byte](bufferSize)
      LazyList.iterate(inputStream.read(buffer)) { length =>
        outputStream.write(buffer, 0, length)
        inputStream.read(buffer)
      }.takeWhile(_ >= 0).take(maxReadIterations)
      new ArraySeq.ofByte(buffer)
    }
  }

  override def notify(request: ArraySeq.ofByte, context: Option[HttpContext]): Identity[Unit] =
    send(request, context)

  private def send(request: ArraySeq.ofByte, context: Option[HttpContext]): HttpURLConnection = {
    val connection = connect()
    val outputStream = connection.getOutputStream
    context.foreach(setProperties(connection, request, _))
    val trySend = Try(outputStream.write(request.unsafeArray))
    context.foreach(clearProperties(connection, _))
    outputStream.close()
    trySend.get
    connection
  }

  private def setProperties(connection: HttpURLConnection, request: ArraySeq.ofByte, context: HttpContext): Unit = {
    // Validate HTTP request properties
    require(httpMethods.contains(context.method), s"Invalid HTTP method: ${context.method}")

    // Set HTTP request context
    connection.setRequestProperty(contentLengthHeader, request.size.toString)
    connection.setRequestProperty(contentTypeHeader, contentType)
    connection.setRequestProperty(acceptHeader, contentType)
    connection.setRequestMethod(context.method)
    connection.setConnectTimeout(context.connectTimeout)
    connection.setReadTimeout(context.readTimeout)
    context.headers.foreach { case (key, value) =>
      connection.setRequestProperty(key, value)
    }
  }

  private def clearProperties(connection: HttpURLConnection, context: HttpContext): Unit =
    context.headers.foreach { case (key, _) =>
      connection.setRequestProperty(key, null)
    }

  private def connect(): HttpURLConnection = {
    // Open new HTTP connection
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection
  }
}

case object UrlConnectionTransport {

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
  case class HttpContext(
    method: String = "POST",
    headers: Map[String, String] = Map.empty,
    followRedirects: Boolean = true,
    connectTimeout: Int = 30000,
    readTimeout: Int = 30000
  )
}
