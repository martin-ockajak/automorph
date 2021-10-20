package automorph.transport.http.client

import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.client.SttpClient.{Context, Protocol, WebSocket}
import automorph.util.Bytes
import java.net.URI
import scala.collection.immutable.ArraySeq
import sttp.capabilities.WebSockets
import sttp.client3.{asByteArrayAlways, asWebSocketAlways, basicRequest, ignore, PartialRequest, Request, Response, SttpBackend}
import sttp.model.{Header, MediaType, Method, Uri}

/**
 * STTP HTTP & WebSocket client message transport plugin.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
 * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
 * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
 * @constructor Creates an STTP client transport plugin with the specified STTP backend.
 * @param url HTTP server endpoint URL
 * @param method HTTP method
 * @param backend STTP backend
 * @param system effect system plugin
 * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
 * @tparam Effect effect type
 */
final case class SttpClient[Effect[_]](
  url: URI,
  method: String,
  backend: SttpBackend[Effect, _],
  system: EffectSystem[Effect],
  webSocket: Boolean = false
) extends ClientMessageTransport[Effect, Context] with Logging {

  private val defaultUrl = Uri(url)
  private val defaultMethod = Method.unsafeApply(method)
  private val protocol = if (webSocket) Protocol.WebSocket else Protocol.Http

  override def call(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    context: Option[Context]
  ): Effect[ArraySeq.ofByte] = {
    val httpRequest = createRequest(requestBody, mediaType, context)
    system.flatMap(
      system.either(send(httpRequest, requestId)),
      (response: Either[Throwable, Response[Array[Byte]]]) => {
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
            logger.debug(s"Received $protocol response", responseProperties + ("Status" -> response.code.toString))
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
    val httpRequest = createRequest(requestBody, mediaType, context).response(ignore)
    system.map(send(httpRequest, requestId), (_: Response[Unit]) => ())
  }

  override def defaultContext: Context = SttpContext.default

  override def close(): Effect[Unit] = backend.close()

  private def send[R](
    httpRequest: Request[R, WebSocket[Effect]],
    requestId: String
  ): Effect[Response[R]] = {
    lazy val requestProperties = Map(
      LogProperties.requestId -> requestId,
      "URL" -> httpRequest.uri.toString,
      "Method" -> httpRequest.method.toString
    )
    logger.trace(s"Sending $protocol httpRequest", requestProperties)
    system.flatMap(
      system.either(httpRequest.send(backend.asInstanceOf[SttpBackend[Effect, WebSocket[Effect]]])),
      (result: Either[Throwable, Response[R]]) =>
        result.fold(
          error => {
            logger.error(s"Failed to send $protocol httpRequest", error, requestProperties)
            system.failed(error)
          },
          response => {
            logger.debug(s"Sent $protocol httpRequest", requestProperties)
            system.pure(response)
          }
        )
    )
  }

  private def sendWebSocket(request: ArraySeq.ofByte): sttp.ws.WebSocket[Effect] => Effect[Array[Byte]] =
    webSocket =>
      system.flatMap(
        webSocket.sendBinary(request.unsafeArray),
        (_: Unit) =>
          webSocket.receiveBinary(true)
      )

  private def createRequest(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): Request[Array[Byte], WebSocket[Effect]] = {
    val http = context.getOrElse(defaultContext)
    val base = http.base.map(_.request).getOrElse(basicRequest)
    val requestUrl = Uri(http.overrideUrl(defaultUrl.toJavaUri))
    val requestMethod = http.method.map(Method.unsafeApply).getOrElse(defaultMethod)
    val contentType = MediaType.unsafeParse(mediaType)
    val headers = base.headers ++ http.headers.map { case (name, value) => Header(name, value) }
    val httpRequest = base.method(requestMethod, requestUrl)
      .contentType(contentType)
      .header(Header.accept(contentType))
      .followRedirects(http.followRedirects.getOrElse(base.options.followRedirects))
      .readTimeout(http.readTimeout.getOrElse(base.options.readTimeout))
      .headers(headers: _*)
      .maxRedirects(base.options.maxRedirects)
    if (webSocket) {
      httpRequest.response(asWebSocketAlways(sendWebSocket(request)))
    } else {
      httpRequest.body(request.unsafeArray).response(asByteArrayAlways)
    }
  }
}

object SttpClient {

  /** STTP backend WebSocket capabilities type. */
  type WebSocket[Effect[_]] = sttp.capabilities.Effect[Effect] with WebSockets

  /** Request context type. */
  type Context = Http[SttpContext]

  /** Transport protocol. */
  private sealed abstract class Protocol(val name: String) {
    override def toString: String = name
  }

  /** Transport protocols. */
  private object Protocol {

    case object Http extends Protocol("HTTP")

    case object WebSocket extends Protocol("WebSocket")
  }
}

final case class SttpContext(request: PartialRequest[Either[String, String], Any])

object SttpContext {
  /** Implicit default context value. */
  implicit val default: Http[SttpContext] = Http()
}
