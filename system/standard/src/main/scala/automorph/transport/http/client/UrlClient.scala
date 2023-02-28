package automorph.transport.http.client

import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{ClientTransport, EffectSystem}
import automorph.transport.http.client.UrlClient.{Context, Session}
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{EffectOps, InputStreamOps, TryOps}
import java.io.InputStream
import java.net.{HttpURLConnection, URI}
import scala.collection.immutable.ListMap
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}
import scala.util.Using

/**
 * Standard JRE HttpURLConnection HTTP client message transport plugin.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol Transport protocol]]
 * @see
 *   [[https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html API]]
 * @constructor
 *   Creates an HttpURLConnection HTTP client message transport plugin.
 * @param effectSystem
 *   effect system plugin
 * @param url
 *   remote API HTTP or WebSocket URL
 * @param method
 *   HTTP request method (default: POST)
 * @tparam Effect
 *   effect type
 */
final case class UrlClient[Effect[_]](effectSystem: EffectSystem[Effect], url: URI, method: HttpMethod = HttpMethod.Post)
  extends ClientTransport[Effect, Context] with Logging {

  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = HttpMethod.values.map(_.name).toSet
  private val log = MessageLog(logger, Protocol.Http.name)
  implicit private val givenSystem: EffectSystem[Effect] = effectSystem
  System.setProperty("sun.net.http.allowRestrictedHeaders", "true")

  override def call(
    requestBody: InputStream,
    requestContext: Context,
    requestId: String,
    mediaType: String,
  ): Effect[(InputStream, Context)] =
    // Send the request
    send(requestBody, requestId, mediaType, requestContext).flatMap { connection =>
      effectSystem.evaluate {
        lazy val responseProperties =
          ListMap(LogProperties.requestId -> requestId, "URL" -> connection.getURL.toExternalForm)

        // Process the response
        log.receivingResponse(responseProperties)
        connection.getResponseCode
        val responseBody = Option(connection.getErrorStream).getOrElse(connection.getInputStream)
        log.receivedResponse(responseProperties + ("Status" -> connection.getResponseCode.toString))
        responseBody -> getResponseContext(connection)
      }
    }

  override def defaultContext: Context =
    Session.defaultContext.url(url).method(method)

  override def close(): Effect[Unit] =
    effectSystem.successful(())

  override def message(
    requestBody: InputStream,
    requestContext: Context,
    requestId: String,
    mediaType: String,
  ): Effect[Unit] =
    send(requestBody, requestId, mediaType, requestContext).map(_ => ())

  private def send(
    request: InputStream,
    requestId: String,
    mediaType: String,
    requestContext: Context,
  ): Effect[HttpURLConnection] =
    effectSystem.evaluate {
      // Create the request
      val requestBody = request.toArray
      val connection = createConnection(requestContext)
      val httpMethod = setConnectionProperties(connection, requestBody, mediaType, requestContext)

      // Log the request
      lazy val requestProperties =
        ListMap(LogProperties.requestId -> requestId, "URL" -> connection.getURL.toExternalForm, "Method" -> httpMethod)
      log.sendingRequest(requestProperties)

      // Send the request
      connection.setDoOutput(true)
      val outputStream = connection.getOutputStream
      val write = Using(outputStream) { stream =>
        stream.write(requestBody)
        stream.flush()
      }
      write.onFailure(error => log.failedSendRequest(error, requestProperties)).get
      log.sentRequest(requestProperties)
      connection
    }

  private def createConnection(requestContext: Context): HttpURLConnection = {
    val requestUrl = requestContext.overrideUrl(requestContext.transport.map(_.connection.getURL.toURI).getOrElse(url))
    requestUrl.toURL.openConnection().asInstanceOf[HttpURLConnection]
  }

  private def setConnectionProperties(
    connection: HttpURLConnection,
    requestBody: Array[Byte],
    mediaType: String,
    requestContext: Context,
  ): String = {
    // Method
    val transportConnection = requestContext.transport.map(_.connection).getOrElse(connection)
    val requestMethod =
      requestContext.method.map(_.name).orElse(requestContext.transport.map(_.connection.getRequestMethod))
        .getOrElse(method.name)
    require(httpMethods.contains(requestMethod), s"Invalid HTTP method: $requestMethod")
    connection.setRequestMethod(requestMethod)

    // Headers
    val transportHeaders = transportConnection.getRequestProperties.asScala.toSeq.flatMap { case (name, values) =>
      values.asScala.map(name -> _)
    }
    (transportHeaders ++ requestContext.headers).foreach { case (name, value) =>
      connection.setRequestProperty(name, value)
    }
    connection.setRequestProperty(contentLengthHeader, requestBody.length.toString)
    connection.setRequestProperty(contentTypeHeader, mediaType)
    connection.setRequestProperty(acceptHeader, mediaType)

    // Timeout & follow redirects
    connection
      .setConnectTimeout(requestContext.timeout.map(_.toMillis.toInt).getOrElse(transportConnection.getConnectTimeout))
    connection.setReadTimeout(
      requestContext.timeout.map {
        case Duration.Inf => 0
        case duration => duration.toMillis.toInt
      }.getOrElse(transportConnection.getReadTimeout)
    )
    connection
      .setInstanceFollowRedirects(
        requestContext.followRedirects.getOrElse(transportConnection.getInstanceFollowRedirects)
      )
    requestMethod
  }

  private def getResponseContext(connection: HttpURLConnection): Context =
    defaultContext.statusCode(connection.getResponseCode).headers(connection.getHeaderFields.asScala.toSeq.flatMap {
      case (name, values) => values.asScala.map(name -> _)
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
