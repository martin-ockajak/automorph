package automorph.transport.http.client

import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.client.HttpClient.{defaultBuilder, Context, Protocol}
import automorph.util.Bytes
import automorph.util.Extensions.TryOps
import java.net.URI
import java.net.http.HttpClient.Redirect
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpRequest, HttpResponse}
import scala.collection.immutable.ArraySeq
import scala.jdk.OptionConverters.RichOptional
import scala.util.Try

/**
 * HttpClient HTTP & WebSocket client message transport plugin.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
 * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
 * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
 * @constructor Creates an HttpClient HTTP & WebSocket message client transport plugin.
 * @param url HTTP server endpoint URL
 * @param method HTTP method
 * @param system effect system plugin
 * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
 * @param builder HttpClient builder
 * @tparam Effect effect type
 */
final case class HttpClient[Effect[_]](
  url: URI,
  method: String,
  system: EffectSystem[Effect],
  webSocket: Boolean = false,
  builder: java.net.http.HttpClient.Builder = defaultBuilder
) extends ClientMessageTransport[Effect, Context] with Logging {

  private val httpClient = builder.build
  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
  private val protocol = if (webSocket) Protocol.WebSocket else Protocol.Http
  require(httpMethods.contains(method), s"Invalid HTTP method: $method")

  override def call(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    context: Option[Context]
  ): Effect[ArraySeq.ofByte] = {
    val httpRequest = createRequest(requestBody, mediaType, context)
    system.flatMap(
      system.either(send(httpRequest, requestId)),
      (response: Either[Throwable, HttpResponse[Array[Byte]]]) => {
        lazy val responseProperties = Map(
          LogProperties.requestId -> requestId,
          "URL" -> httpRequest.uri.toString
        )
        response.fold(
          error => {
            logger.error(s"Failed to receive $protocol response", error, responseProperties)
            system.failed(error)
          },
          response => {
            logger.debug(s"Received $protocol response", responseProperties + ("Status" -> response.statusCode.toString))
            system.pure(Bytes.byteArray.from(response.body))
          }
        )
      }
    )
  }

  override def notify(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    context: Option[Context]
  ): Effect[Unit] = {
    val httpRequest = createRequest(requestBody, mediaType, context)
    system.map(send(httpRequest, requestId), (_: HttpResponse[Array[Byte]]) => ())
  }

  override def defaultContext: Context = HttpContext.default

  override def close(): Effect[Unit] = system.wrap(())

  private def send(
    httpRequest: HttpRequest,
    requestId: String
  ): Effect[HttpResponse[Array[Byte]]] = system.wrap {
    lazy val requestProperties = Map(
      LogProperties.requestId -> requestId,
      "URL" -> httpRequest.uri.toString,
      "Method" -> httpRequest.method
    )
    logger.trace(s"Sending $protocol httpRequest", requestProperties)
    Try(httpClient.send(httpRequest, BodyHandlers.ofByteArray)).pureFold(
      error => {
        logger.error(s"Failed to send $protocol httpRequest", error, requestProperties)
        throw error
      },
      response => {
        logger.debug(s"Sent $protocol httpRequest", requestProperties)
        response
      }
    )
  }

  private def createRequest(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): HttpRequest = {
    val http = context.getOrElse(defaultContext)
    val requestUrl = http.overrideUrl(url)
    val requestMethod = http.method.getOrElse(method)
    require(httpMethods.contains(requestMethod), s"Invalid HTTP method: $method")
    val base = http.base.map(_.request).getOrElse(HttpRequest.newBuilder.uri(requestUrl))
    val headers = http.headers.map { case (name, value) => Seq(name, value) }.flatten.toArray
    val httpRequestBuilder = base.uri(requestUrl).method(requestMethod, BodyPublishers.ofByteArray(request.unsafeArray))
      .header(contentTypeHeader, mediaType)
      .header(acceptHeader, mediaType)
      .headers(headers*)
    http.readTimeout.map { timeout =>
      java.time.Duration.ofMillis(timeout.toMillis)
    }.orElse(base.build.timeout.toScala).map { timeout =>
      httpRequestBuilder.timeout(timeout)
    }.getOrElse(httpRequestBuilder).build
//    if (webSocket) {
//      httpRequest.response(asWebSocketAlways(sendWebSocket(request)))
//    } else {
//    }
  }
}

object HttpClient {

  /** Request context type. */
  type Context = Http[HttpContext]

  /** Transport protocol. */
  sealed abstract private class Protocol(val name: String) {
    override def toString: String = name
  }

  /** Transport protocols. */
  private object Protocol {

    case object Http extends Protocol("HTTP")

    case object WebSocket extends Protocol("WebSocket")
  }

  val defaultBuilder = java.net.http.HttpClient.newBuilder
}

final case class HttpContext(request: HttpRequest.Builder)

object HttpContext {
  /** Implicit default context value. */
  implicit val default: Http[HttpContext] = Http()
}
