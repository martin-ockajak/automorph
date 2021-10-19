package automorph.transport.http.client

import automorph.log.{LogProperties, Logging}
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
import scala.util.Using

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

  override def call(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    context: Option[Context]
  ): Effect[ArraySeq.ofByte] = {
    val http = context.getOrElse(defaultContext)
    system.flatMap(
      send(requestBody, requestId, mediaType, http),
      (_: EffectValue) match {
        case (connection: HttpURLConnection, _) =>
          system.wrap {
            lazy val responseProperties = Map(
              LogProperties.requestId -> requestId,
              "URL" -> url
            )
            logger.trace("Receiving HTTP response", responseProperties)
            connection.getResponseCode
            val inputStream = Option(connection.getErrorStream).getOrElse(connection.getInputStream)
            val response = Using(inputStream)(Bytes.inputStream.from).onFailure {
              logger.error("Failed to receive HTTP response", _, responseProperties)
            }.get
            logger.debug("Received HTTP response", responseProperties + ("Status" -> connection.getResponseCode.toString))
            response
          }
      }
    )
  }

  override def notify(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    context: Option[Context]
  ): Effect[Unit] = {
    val http = context.getOrElse(defaultContext)
    system.map(send(requestBody, requestId, mediaType, http), (_: EffectValue) => ())
  }

  override def defaultContext: Context = HttpUrlConnectionContext.default

  override def close(): Effect[Unit] = system.pure(())

  private def send(request: ArraySeq.ofByte, requestId: String, mediaType: String, context: Context): Effect[EffectValue] =
    system.wrap {
      val httpMethod = determineMethod(context)
      lazy val requestProperties = Map(
        LogProperties.requestId -> requestId,
        "URL" -> url,
        "Method" -> httpMethod
      )
      logger.trace("Sending HTTP request", requestProperties)
      val connection = createConnection(context)
      setRequestProperties(connection, request, mediaType, httpMethod, context)
      connection.setDoOutput(true)
      val outputStream = connection.getOutputStream
      val write = Using(outputStream) { stream =>
        stream.write(request.unsafeArray)
        stream.flush()
      }
      write.onFailure(logger.error("Failed to send HTTP request", _, requestProperties)).get
      logger.debug("Sent HTTP request", requestProperties)
      connection -> new ArraySeq.ofByte(Array.empty)
    }

  private def determineMethod(http: Context): String =
    http.method.orElse(http.base.map(_.connection.getRequestMethod)).getOrElse(method)

  private def setRequestProperties(
    connection: HttpURLConnection,
    request: ArraySeq.ofByte,
    mediaType: String,
    httpMethod: String,
    http: Context
  ): Unit = {
    val default = http.base.map(_.connection).getOrElse(connection)
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
  }

  private def createConnection(http: Context): HttpURLConnection = {
    val connectionUrl = http.url.orElse(http.base.map(_.connection.getURL.toURI)).getOrElse(url)
    connectionUrl.toURL.openConnection().asInstanceOf[HttpURLConnection]
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
  private type EffectValue = (HttpURLConnection, ArraySeq.ofByte)
}

final case class HttpUrlConnectionContext(connection: HttpURLConnection)

object HttpUrlConnectionContext {
  /** Implicit default context value. */
  implicit val default: Http[HttpUrlConnectionContext] = Http()
}
