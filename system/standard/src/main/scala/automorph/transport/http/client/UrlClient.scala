package automorph.transport.http.client

import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.http.HttpContext
import automorph.transport.http.client.UrlClient.{Context, EffectValue}
import automorph.util.Bytes
import automorph.util.Extensions.TryOps
import java.net.{HttpURLConnection, URI}
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}
import scala.util.Using

/**
 * HttpURLConnection HTTP client message transport plugin.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol Transport protocol]]
 * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html API]]
 * @constructor Creates an HttpURLConnection HTTP client message transport plugin.
 * @param url HTTP server endpoint URL
 * @param method HTTP method
 * @param system effect system plugin
 * @tparam Effect effect type
 */
final case class UrlClient[Effect[_]](
  url: URI,
  method: String,
  system: EffectSystem[Effect]
) extends ClientMessageTransport[Effect, Context] with Logging {

  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
  require(httpMethods.contains(method), s"Invalid HTTP method: $method")
  System.setProperty("sun.net.http.allowRestrictedHeaders", "true")

  override def call(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    requestContext: Option[Context]
  ): Effect[(ArraySeq.ofByte, Context)] =
    // Send the request
    system.flatMap(
      send(requestBody, requestId, mediaType, requestContext),
      (_: EffectValue) match {
        case (connection: HttpURLConnection, _) =>
          system.wrap {
            lazy val responseProperties = Map(
              LogProperties.requestId -> requestId,
              "URL" -> connection.getURL.toExternalForm
            )

            // Process the response
            logger.trace("Receiving HTTP response", responseProperties)
            connection.getResponseCode
            val inputStream = Option(connection.getErrorStream).getOrElse(connection.getInputStream)
            val response = Using(inputStream)(Bytes.inputStream.from).onFailure {
              logger.error("Failed to receive HTTP response", _, responseProperties)
            }.get
            logger.debug("Received HTTP response", responseProperties + ("Status" -> connection.getResponseCode.toString))
            response -> responseContext(connection)
          }
      }
    )

  override def notify(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    requestContext: Option[Context]
  ): Effect[Unit] =
    system.map(send(requestBody, requestId, mediaType, requestContext), (_: EffectValue) => ())

  override def defaultContext: Context =
    UrlContext.default

  override def close(): Effect[Unit] =
    system.pure(())

  private def send(
    request: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    context: Option[Context]
  ): Effect[EffectValue] =
    system.wrap {
      // Create the request
      val connection = createConnection(context)
      val httpMethod = setConnectionProperties(connection, request, mediaType, context)

      // Log the request
      lazy val requestProperties = Map(
        LogProperties.requestId -> requestId,
        "URL" -> connection.getURL.toExternalForm,
        "Method" -> httpMethod
      )
      logger.trace("Sending HTTP request", requestProperties)

      // Send the request
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

  private def createConnection(context: Option[Context]): HttpURLConnection = {
    val http = context.getOrElse(defaultContext)
    val baseUrl = http.base.map(_.connection.getURL.toURI).getOrElse(url)
    val requestUrl = http.overrideUrl(baseUrl)
    requestUrl.toURL.openConnection().asInstanceOf[HttpURLConnection]
  }

  private def setConnectionProperties(
    connection: HttpURLConnection,
    requestBody: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): String = {
    val http = context.getOrElse(defaultContext)
    val baseConnection = http.base.map(_.connection).getOrElse(connection)
    val requestMethod = http.method.orElse(http.base.map(_.connection.getRequestMethod)).getOrElse(method)
    require(httpMethods.contains(requestMethod), s"Invalid HTTP method: $requestMethod")
    connection.setRequestMethod(requestMethod)
    val baseHeaders = baseConnection.getRequestProperties.asScala.toSeq.flatMap { case (name, values) =>
      values.asScala.map(name -> _)
    }
    (baseHeaders ++ http.headers).foreach { case (name, value) =>
      connection.setRequestProperty(name, value)
    }
    connection.setRequestProperty(contentLengthHeader, requestBody.size.toString)
    connection.setRequestProperty(contentTypeHeader, mediaType)
    connection.setRequestProperty(acceptHeader, mediaType)
    connection.setInstanceFollowRedirects(http.followRedirects.getOrElse(baseConnection.getInstanceFollowRedirects))
    connection.setConnectTimeout(http.readTimeout.map(_.toMillis.toInt).getOrElse(baseConnection.getConnectTimeout))
    connection.setReadTimeout(http.readTimeout.map {
      case Duration.Inf => 0
      case duration => duration.toMillis.toInt
    }.getOrElse(baseConnection.getReadTimeout))
    requestMethod
  }

  private def responseContext(connection: HttpURLConnection): Context =
    defaultContext.statusCode(connection.getResponseCode)
      .headers(connection.getHeaderFields.asScala.toSeq.flatMap { case (name, values) =>
        values.asScala.map(name -> _)
      }*)
}

object UrlClient {

  /** Request context type. */
  type Context = HttpContext[UrlContext]

  /** Effect value type. */
  private type EffectValue = (HttpURLConnection, ArraySeq.ofByte)
}

final case class UrlContext(connection: HttpURLConnection)

object UrlContext {
  /** Implicit default context value. */
  implicit val default: HttpContext[UrlContext] = HttpContext()
}
