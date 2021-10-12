package automorph.transport.http.client

import automorph.log.Logging
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.client.HttpUrlConnectionClient.{Context, EffectValue}
import automorph.util.Bytes
import automorph.util.Extensions.TryOps
import java.net.{HttpURLConnection, URI}
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}
import scala.util.{Try, Using}

/**
 * URL connection HTTP client message transport plugin.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol Transport protocol]]
 * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html API]]
 * @constructor Creates an HTTP URL connection client transport plugin.
 * @param url HTTP server endpoint URL
 * @param method HTTP method
 * @param system effect system plugin
 * @tparam Effect effect type
 */
final case class HttpUrlConnectionClient[Effect[_]](
  url: URI,
  method: String,
  system: EffectSystem[Effect]
) extends ClientMessageTransport[Effect, Context] with Logging {

  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
  require(httpMethods.contains(method), s"Invalid HTTP method: $method")

  override def call(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[ArraySeq.ofByte] =
    system.flatMap(
      send(request, mediaType, context),
      (_: EffectValue) match {
        case (connection: HttpURLConnection, _) =>
          system.wrap {
            logger.trace("Receiving HTTP response", Map("URL" -> url))
            val response = Try(Using.resource(connection.getInputStream)(Bytes.inputStream.from)).mapFailure { error =>
              logger.error("Failed to receive HTTP response", error, Map("URL" -> url))
              error
            }.get
            logger.debug(
              "Received HTTP response",
              Map("URL" -> url, "Status" -> connection.getResponseCode, "Size" -> response.length)
            )
            clearRequestProperties(connection, context.getOrElse(defaultContext))
            response
          }
      }
    )

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[Unit] =
    system.map(send(request, mediaType, context), (_: (HttpURLConnection, ArraySeq.ofByte)) => ())

  override def defaultContext: Context = HttpUrlConnectionContext.default

  override def close(): Effect[Unit] = system.pure(())

  private def send(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[EffectValue] =
    system.wrap {
      logger.trace("Sending HTTP request", Map("URL" -> url, "Size" -> request.length))
      val http = context.getOrElse(defaultContext)
      val connection = connect(http)
      val httpMethod = setRequestProperties(connection, request, mediaType, http)
      val outputStream = connection.getOutputStream
      val write = Using(outputStream)(_.write(request.unsafeArray))
      write.mapFailure { error =>
        logger.error(
          "Failed to send HTTP request",
          error,
          Map("URL" -> url, "Method" -> httpMethod, "Size" -> request.length)
        )
        error
      }.get
      logger.debug("Sent HTTP request", Map("URL" -> url, "Method" -> httpMethod, "Size" -> request.length))
      connection -> new ArraySeq.ofByte(Array.empty)
    }

  private def setRequestProperties(
    connection: HttpURLConnection,
    request: ArraySeq.ofByte,
    mediaType: String,
    http: Context
  ): String = {
    val default = http.base.map(_.connection).getOrElse(connection)
    val httpMethod = http.method.orElse(http.base.map(_.connection.getRequestMethod)).getOrElse(method)
    require(httpMethods.contains(httpMethod), s"Invalid HTTP method: $httpMethod")
    connection.setRequestMethod(httpMethod)
    connection.setInstanceFollowRedirects(http.followRedirects.getOrElse(default.getInstanceFollowRedirects))
    connection.setConnectTimeout(http.readTimeout.map(_.toMillis.toInt).getOrElse(default.getConnectTimeout))
    connection.setReadTimeout(http.readTimeout.map {
      case Duration.Inf => 0
      case duration => duration.toMillis.toInt
    }.getOrElse(default.getReadTimeout))
    connection.setRequestProperty(contentLengthHeader, request.size.toString)
    connection.setRequestProperty(contentTypeHeader, mediaType)
    connection.setRequestProperty(acceptHeader, mediaType)
    (connectionHeaders(default) ++ http.headers).foreach { case (name, value) =>
      connection.setRequestProperty(name, value)
    }
    httpMethod
  }

  private def clearRequestProperties(connection: HttpURLConnection, http: Context): Unit =
    (connectionHeaders(connection) ++ http.headers).foreach { case (name, _) =>
      connection.setRequestProperty(name, null)
    }

  private def connect(http: Context): HttpURLConnection = {
    val connectionUrl = http.url.orElse(http.base.map(_.connection.getURL.toURI)).getOrElse(url)
    val connection = connectionUrl.toURL.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection
  }

  private def connectionHeaders(connection: HttpURLConnection): Seq[(String, String)] =
    connection.getRequestProperties.asScala.toSeq.flatMap { case (name, values) =>
      values.asScala.map(name -> _)
    }
}

object HttpUrlConnectionClient {

  /** Request context type. */
  type Context = Http[HttpUrlConnectionContext]

  /** Effect value type. */
  private[automorph] type EffectValue = (HttpURLConnection, ArraySeq.ofByte)
}

final case class HttpUrlConnectionContext(connection: HttpURLConnection)

object HttpUrlConnectionContext {
  /** Implicit default context value. */
  implicit val default: Context = Http()
}
