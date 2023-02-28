package automorph.transport.http.client

import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{ClientTransport, EffectSystem}
import automorph.transport.http.client.SttpClient.{Context, Session}
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{ByteArrayOps, EffectOps, InputStreamOps}
import java.io.InputStream
import java.net.URI
import scala.collection.immutable.ListMap
import sttp.capabilities.WebSockets
import sttp.client3.{PartialRequest, Request, Response, SttpBackend, asByteArrayAlways, asWebSocketAlways, basicRequest, ignore}
import sttp.model.{Header, MediaType, Method, Uri}

/**
 * STTP HTTP & WebSocket client message transport plugin.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
 * @see
 *   [[https://sttp.softwaremill.com/en/latest Library documentation]]
 * @see
 *   [[https://javadoc.io/doc/com.softwaremill.sttp.client3/core_3/latest/index.html API]]
 * @constructor
 *   Creates an STTP HTTP & WebSocket client message transport plugin with the specified STTP backend.
 * @param effectSystem
 *   effect system plugin
 * @param backend
 *   STTP backend
 * @param url
 *   remote API HTTP or WebSocket URL
 * @param method
 *   HTTP request method
 * @param webSocket
 *   true if WebSocket protocol is supported, false otherwise
 * @tparam Effect
 *   effect type
 */
final case class SttpClient[Effect[_]] private (
  effectSystem: EffectSystem[Effect],
  backend: SttpBackend[Effect, ?],
  url: URI,
  method: HttpMethod,
  webSocket: Boolean,
) extends ClientTransport[Effect, Context] with Logging {

  private type WebSocket = sttp.capabilities.Effect[Effect] & WebSockets

  private val webSocketsSchemePrefix = "ws"
  private val defaultUrl = Uri(url).toJavaUri
  private val log = MessageLog(logger, Protocol.Http.name)
  implicit private val givenSystem: EffectSystem[Effect] = effectSystem

  override def call(
    requestBody: InputStream,
    requestContext: Context,
    requestId: String,
    mediaType: String,
  ): Effect[(InputStream, Context)] = {
    // Send the request
    val sttpRequest = createRequest(requestBody, mediaType, requestContext)
    transportProtocol(sttpRequest).flatMap { protocol =>
      send(sttpRequest, requestId, protocol).either.flatMap { result =>
        lazy val responseProperties = ListMap(LogProperties.requestId -> requestId, "URL" -> sttpRequest.uri.toString)

        // Process the response
        result.fold(
          error => {
            log.failedReceiveResponse(error, responseProperties, protocol.name)
            effectSystem.failed(error)
          },
          response => {
            log.receivedResponse(responseProperties + ("Status" -> response.code.toString), protocol.name)
            effectSystem.successful(response.body.toInputStream -> getResponseContext(response))
          },
        )
      }
    }
  }

  override def message(
    requestBody: InputStream,
    requestContext: Context,
    requestId: String,
    mediaType: String,
  ): Effect[Unit] = {
    val sttpRequest = createRequest(requestBody, mediaType, requestContext)
    transportProtocol(sttpRequest).flatMap {
      case Protocol.Http => send(sttpRequest.response(ignore), requestId, Protocol.Http).map(_ => ())
      case Protocol.WebSocket => send(sttpRequest, requestId, Protocol.WebSocket).map(_ => ())
    }
  }

  override def defaultContext: Context =
    Session.defaultContext.url(url).method(method)

  override def close(): Effect[Unit] =
    backend.close()

  private def send[R](
    sttpRequest: Request[R, WebSocket],
    requestId: String,
    protocol: Protocol,
  ): Effect[Response[R]] = {
    // Log the request
    lazy val requestProperties = ListMap(LogProperties.requestId -> requestId, "URL" -> sttpRequest.uri.toString) ++
      Option.when(protocol == Protocol.Http)("Method" -> sttpRequest.method.toString)
    log.sendingRequest(requestProperties, protocol.name)

    // Send the request
    sttpRequest.send(backend.asInstanceOf[SttpBackend[Effect, WebSockets]]).either.flatMap(
      _.fold(
        error => {
          log.failedSendRequest(error, requestProperties, protocol.name)
          effectSystem.failed(error)
        },
        response => {
          log.sentRequest(requestProperties, protocol.name)
          effectSystem.successful(response)
        },
      )
    )
  }

  private def createRequest(
    requestBody: InputStream,
    mediaType: String,
    requestContext: Context,
  ): Request[Array[Byte], WebSocket] = {
    // URL & method
    val transportRequest = requestContext.transport.map(_.request).getOrElse(basicRequest)
    val requestUrl = Uri(requestContext.overrideUrl(defaultUrl))
    val requestMethod = Method.unsafeApply(requestContext.method.getOrElse(method).name)

    // Headers, timeout & follow redirects
    val contentType = MediaType.unsafeParse(mediaType)
    val sttpRequest = transportRequest.method(requestMethod, requestUrl).headers(requestContext.headers.map {
      case (name, value) => Header(name, value)
    }*).contentType(contentType).header(Header.accept(contentType))
      .readTimeout(requestContext.timeout.getOrElse(transportRequest.options.readTimeout))
      .followRedirects(requestContext.followRedirects.getOrElse(transportRequest.options.followRedirects))
      .maxRedirects(transportRequest.options.maxRedirects)

    // Body & response type
    requestUrl.toString.toLowerCase match {
      case scheme if scheme.startsWith(webSocketsSchemePrefix) =>
        // Create WebSocket request
        sttpRequest.response(asWebSocketAlways(sendWebSocket(requestBody)))
      case _ =>
        // Create HTTP request
        sttpRequest.body(requestBody.toArray).response(asByteArrayAlways)
    }
  }

  private def sendWebSocket(request: InputStream): sttp.ws.WebSocket[Effect] => Effect[Array[Byte]] =
    webSocket => webSocket.sendBinary(request.toArray).flatMap(_ => webSocket.receiveBinary(true))

  private def getResponseContext(response: Response[Array[Byte]]): Context =
    defaultContext.statusCode(response.code.code).headers(response.headers.map { header =>
      header.name -> header.value
    }*)

  private def transportProtocol(sttpRequest: Request[Array[Byte], WebSocket]): Effect[Protocol] =
    if (sttpRequest.isWebSocket) {
      if (webSocket) { effectSystem.successful(Protocol.WebSocket) }
      else {
        effectSystem.failed(
          throw new IllegalArgumentException(
            s"Selected STTP backend does not support WebSocket: ${backend.getClass.getSimpleName}"
          )
        )
      }
    } else effectSystem.successful(Protocol.Http)
}

object SttpClient {

  /** Request context type. */
  type Context = HttpContext[Session]

  /**
   * Creates an STTP HTTP & WebSocket client message transport plugin with the specified STTP backend.
   *
   * @param system
   *   effect system plugin
   * @param backend
   *   STTP backend
   * @param url
   *   HTTP or WebSocket server endpoint URL
   * @param method
   *   HTTP request method (default: POST)
   * @tparam Effect
   *   effect type
   * @return
   *   STTP HTTP & WebSocket client message transport plugin
   */
  def apply[Effect[_]](
    system: EffectSystem[Effect],
    backend: SttpBackend[Effect, ?],
    url: URI,
    method: HttpMethod = HttpMethod.Post,
  ): SttpClient[Effect] =
    SttpClient[Effect](system, backend, url, method, webSocket = true)

  /**
   * Creates an STTP HTTP client message transport plugin with the specified STTP backend.
   *
   * @param system
   *   effect system plugin
   * @param backend
   *   STTP backend
   * @param url
   *   HTTP or WebSocket server endpoint URL
   * @param method
   *   HTTP request method (default: POST)
   * @tparam Effect
   *   effect type
   * @return
   *   STTP HTTP client message transport plugin
   */
  def http[Effect[_]](
    system: EffectSystem[Effect],
    backend: SttpBackend[Effect, ?],
    url: URI,
    method: HttpMethod = HttpMethod.Post,
  ): SttpClient[Effect] =
    SttpClient[Effect](system, backend, url, method, webSocket = false)

  final case class Session(request: PartialRequest[Either[String, String], Any])

  object Session {

    /** Implicit default context value. */
    implicit val defaultContext: HttpContext[Session] = HttpContext()
  }
}
