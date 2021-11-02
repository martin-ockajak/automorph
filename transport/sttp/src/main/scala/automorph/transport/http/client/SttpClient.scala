package automorph.transport.http.client

import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.http.HttpContext
import automorph.transport.http.client.SttpClient.{Context, Protocol, Session, WebSocket}
import automorph.util.Bytes
import automorph.util.Extensions.EffectOps
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
 * @constructor Creates an STTP HTTP & WebSocket client message transport plugin with the specified STTP backend.
 * @param url HTTP or WebSocket server endpoint URL
 * @param method HTTP request method
 * @param backend STTP backend
 * @param system effect system plugin
 * @param webSocketSupport true if WebSocket protocol is supported, false otherwise
 * @tparam Effect effect type
 */
final case class SttpClient[Effect[_]] private (
  url: URI,
  method: String,
  backend: SttpBackend[Effect, _],
  system: EffectSystem[Effect],
  webSocketSupport: Boolean
) extends ClientMessageTransport[Effect, Context] with Logging {

  private val webSocketsSchemePrefix = "ws"
  private val defaultUrl = Uri(url)
  private val defaultMethod = Method.unsafeApply(method)
  implicit private val givenSystem: EffectSystem[Effect] = system

  override def call(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    requestContext: String,
    context: Option[Context]
  ): Effect[(ArraySeq.ofByte, Context)] = {
    // Send the request
    val sttpRequest = createRequest(requestBody, requestContext, context)
    transportProtocol(sttpRequest).flatMap { protocol =>
      send(sttpRequest, requestId, protocol).either.flatMap { result =>
        lazy val responseProperties = Map(
          LogProperties.requestId -> requestId,
          "URL" -> sttpRequest.uri.toString
        )

        // Process the response
        result.fold(
          error => {
            logger.error(s"Failed to receive $protocol response", error, responseProperties)
            system.failed(error)
          },
          response => {
            logger.debug(s"Received $protocol response", responseProperties + ("Status" -> response.code.toString))
            system.pure(Bytes.byteArray.from(response.body) -> responseContext(response))
          }
        )
      }
    }
  }

  override def notify(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    requestContext: Option[Context]
  ): Effect[Unit] = {
    val sttpRequest = createRequest(requestBody, mediaType, requestContext)
    transportProtocol(sttpRequest).flatMap { protocol =>
      send(sttpRequest.response(ignore), requestId, protocol).map(_ => ())
    }
  }

  override def defaultContext: Context =
    Session.default

  override def close(): Effect[Unit] =
    backend.close()

  private def send[R](
    sttpRequest: Request[R, WebSocket[Effect]],
    requestId: String,
    protocol: Protocol
  ): Effect[Response[R]] = {
    // Log the request
    lazy val requestProperties = Map(
      LogProperties.requestId -> requestId,
      "URL" -> sttpRequest.uri.toString
    ) ++ Option.when(protocol == Protocol.Http)("Method" -> sttpRequest.method.toString)
    logger.trace(s"Sending $protocol request", requestProperties)

    // Send the request
    sttpRequest.send(backend.asInstanceOf[SttpBackend[Effect, WebSockets]]).either.flatMap(_.fold(
      error => {
        logger.error(s"Failed to send $protocol request", error, requestProperties)
        system.failed(error)
      },
      response => {
        logger.debug(s"Sent $protocol request", requestProperties)
        system.pure(response)
      }
    ))
  }

  private def sendWebSocket(request: ArraySeq.ofByte): sttp.ws.WebSocket[Effect] => Effect[Array[Byte]] =
    webSocket =>
      webSocket.sendBinary(request.unsafeArray).flatMap(_ =>
        webSocket.receiveBinary(true)
      )

  private def createRequest(
    requestBody: ArraySeq.ofByte,
    mediaType: String,
    requestContext: Option[Context]
  ): Request[Array[Byte], WebSocket[Effect]] = {
    val httpContext = requestContext.getOrElse(defaultContext)
    val baseRequest = httpContext.base.map(_.request).getOrElse(basicRequest)
    val requestUrl = Uri(httpContext.overrideUrl(defaultUrl.toJavaUri))
    val requestMethod = httpContext.method.map(Method.unsafeApply).getOrElse(defaultMethod)
    val contentType = MediaType.unsafeParse(mediaType)
    val headers = baseRequest.headers ++ httpContext.headers.map { case (name, value) => Header(name, value) }
    val sttpRequest = baseRequest.method(requestMethod, requestUrl)
      .headers(headers*)
      .contentType(contentType)
      .header(Header.accept(contentType))
      .followRedirects(httpContext.followRedirects.getOrElse(baseRequest.options.followRedirects))
      .readTimeout(httpContext.readTimeout.getOrElse(baseRequest.options.readTimeout))
      .maxRedirects(baseRequest.options.maxRedirects)
    requestUrl.toString.toLowerCase match {
      case scheme if scheme.startsWith(webSocketsSchemePrefix) =>
        // Create WebSocket request
        sttpRequest.response(asWebSocketAlways(sendWebSocket(requestBody)))
      case _ =>
        // Create HTTP request
        sttpRequest.body(requestBody.unsafeArray).response(asByteArrayAlways)
    }
  }

  private def responseContext(response: Response[Array[Byte]]): Context =
    defaultContext.statusCode(response.code.code).headers(response.headers.map { header =>
      header.name -> header.value
    }*)

  private def transportProtocol(sttpRequest: Request[Array[Byte], WebSocket[Effect]]): Effect[Protocol] = {
    if (sttpRequest.isWebSocket) {
      if (webSocketSupport) {
        system.pure(Protocol.WebSocket)
      } else {
        system.failed(
          throw IllegalArgumentException(s"Selected STTP backend does not support WebSocket: ${backend.getClass.getSimpleName}")
        )
      }
    } else system.pure(Protocol.Http)
  }
}

object SttpClient {

  /** Request context type. */
  type Context = HttpContext[Session]

  /**
   * Creates an STTP HTTP & WebSocket client message transport plugin with the specified STTP backend.
   *
   * @param url HTTP or WebSocket server endpoint URL
   * @param method HTTP request method
   * @param backend STTP backend
   * @param system effect system plugin
   * @tparam Effect effect type
   * @return
   */
  def apply[Effect[_]](
    url: URI,
    method: String,
    backend: SttpBackend[Effect, WebSockets],
    system: EffectSystem[Effect]
  ): SttpClient[Effect] =
    SttpClient[Effect](url, method, backend, system, true)

  /**
   * Creates an STTP HTTP client message transport plugin with the specified STTP backend.
   *
   * @param url HTTP or WebSocket server endpoint URL
   * @param method HTTP request method
   * @param backend STTP backend
   * @param system effect system plugin
   * @tparam Effect effect type
   * @return
   */
  def http[Effect[_]](
    url: URI,
    method: String,
    backend: SttpBackend[Effect, _],
    system: EffectSystem[Effect]
  ): SttpClient[Effect] =
    SttpClient[Effect](url, method, backend, system, false)

  /** STTP backend WebSocket capabilities type. */
  private type WebSocket[Effect[_]] = sttp.capabilities.Effect[Effect] with WebSockets

  /** Transport protocol. */
  sealed abstract private class Protocol(val name: String) {
    override def toString: String = name
  }

  /** Transport protocols. */
  private object Protocol {

    case object Http extends Protocol("HTTP")

    case object WebSocket extends Protocol("WebSocket")
  }

  final case class Session(request: PartialRequest[Either[String, String], Any])

  object Session {
    /** Implicit default context value. */
    implicit val default: HttpContext[Session] = HttpContext()
  }
}
