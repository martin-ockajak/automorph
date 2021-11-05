package automorph.transport.http.client

import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.http.{HttpContext, HttpMethod}
import automorph.transport.http.client.UrlClient.{Context, Session}
import automorph.util.Bytes
import automorph.util.Extensions.{EffectOps, TryOps}
import java.net.{HttpURLConnection, URI}
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}
import scala.util.Using

/**
 * Standard JRE HttpURLConnection HTTP client message transport plugin.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol Transport protocol]]
 * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html API]]
 * @constructor Creates an HttpURLConnection HTTP client message transport plugin.
 * @param system effect system plugin
 * @param url remote API HTTP or WebSocket URL
 * @param method HTTP request method (default: POST)
 * @tparam Effect effect type
 */
final case class UrlClient[Effect[_]](
  system: EffectSystem[Effect],
  url: URI,
  method: HttpMethod = HttpMethod.Post
) extends ClientMessageTransport[Effect, Context] with Logging {

  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = HttpMethod.values.map(_.name).toSet
  implicit private val givenSystem: EffectSystem[Effect] = system
  System.setProperty("sun.net.http.allowRestrictedHeaders", "true")

  override def call(
    requestBody: ArraySeq.ofByte,
    requestContext: Option[Context],
    requestId: String,
    mediaType: String
  ): Effect[(ArraySeq.ofByte, Context)] =
    // Send the request
    send(requestBody, requestId, mediaType, requestContext).flatMap { connection =>
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
        response -> getResponseContext(connection)
      }
    }

  override def message(
    requestBody: ArraySeq.ofByte,
    requestContext: Option[Context],
    requestId: String,
    mediaType: String
  ): Effect[Unit] =
    send(requestBody, requestId, mediaType, requestContext).map(_ => ())

  override def defaultContext: Context =
    Session.defaultContext

  override def close(): Effect[Unit] =
    system.pure(())

  private def send(
    request: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    context: Option[Context]
  ): Effect[HttpURLConnection] =
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
      connection
    }

  private def createConnection(context: Option[Context]): HttpURLConnection = {
    val httpContext = context.getOrElse(defaultContext)
    val requestUrl = httpContext.overrideUrl {
      httpContext.transport.map(_.connection.getURL.toURI).getOrElse(url)
    }
    requestUrl.toURL.openConnection().asInstanceOf[HttpURLConnection]
  }

  private def setConnectionProperties(
    connection: HttpURLConnection,
    requestBody: ArraySeq.ofByte,
    mediaType: String,
    requestContext: Option[Context]
  ): String = {
    // Method
    val httpContext = requestContext.getOrElse(defaultContext)
    val transportConnection = httpContext.transport.map(_.connection).getOrElse(connection)
    val requestMethod = httpContext.method.map(_.name).orElse(
      httpContext.transport.map(_.connection.getRequestMethod)
    ).getOrElse(method.name)
    require(httpMethods.contains(requestMethod), s"Invalid HTTP method: $requestMethod")
    connection.setRequestMethod(requestMethod)

    // Headers
    val transportHeaders = transportConnection.getRequestProperties.asScala.toSeq.flatMap { case (name, values) =>
      values.asScala.map(name -> _)
    }
    (transportHeaders ++ httpContext.headers).foreach { case (name, value) =>
      connection.setRequestProperty(name, value)
    }
    connection.setRequestProperty(contentLengthHeader, requestBody.size.toString)
    connection.setRequestProperty(contentTypeHeader, mediaType)
    connection.setRequestProperty(acceptHeader, mediaType)

    // Timeout & follow redirects
    connection.setConnectTimeout(
      httpContext.timeout.map(_.toMillis.toInt).getOrElse(transportConnection.getConnectTimeout)
    )
    connection.setReadTimeout(httpContext.timeout.map {
      case Duration.Inf => 0
      case duration => duration.toMillis.toInt
    }.getOrElse(transportConnection.getReadTimeout))
    connection.setInstanceFollowRedirects(
      httpContext.followRedirects.getOrElse(transportConnection.getInstanceFollowRedirects)
    )
    requestMethod
  }

  private def getResponseContext(connection: HttpURLConnection): Context =
    defaultContext.statusCode(connection.getResponseCode)
      .headers(connection.getHeaderFields.asScala.toSeq.flatMap { case (name, values) =>
        values.asScala.map(name -> _)
      }*)
}

object UrlClient {

  /** Request context type. */
  type Context = HttpContext[Session]

  final case class Session(connection: HttpURLConnection)

  object Session {
    /** Implicit default context value. */
    implicit val defaultContext: HttpContext[Session] = HttpContext()
  }
}
