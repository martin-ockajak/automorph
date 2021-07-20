package automorph.transport.http.client

import automorph.log.Logging
import automorph.spi.{ClientMessageTransport, EffectSystem}
import automorph.transport.http.HttpProperties
import automorph.transport.http.client.SttpClient.Context
import sttp.capabilities.WebSockets
import automorph.util.Bytes
import java.net.URI
import scala.collection.immutable.ArraySeq
import sttp.capabilities
import sttp.client3.{asByteArrayAlways, asWebSocketAlways, basicRequest, ignore, Identity, PartialRequest, Request, Response, SttpBackend}
import sttp.model.{Header, MediaType, Method, Uri}
import sttp.ws.WebSocket

/**
 * STTP client transport plugin using HTTP as message transport protocol with the specified STTP backend.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see [[https://sttp.softwaremill.com/en/latest/ Documentation]]
 * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
 * @constructor Creates an STTP client transport plugin with the specified STTP backend.
 * @param url endpoint URL
 * @param method HTTP method, upgrade the HTTP connection to WebSocket protocol if empty
 * @param system effect system plugin
 * @param method HTTP method
 * @param backend STTP backend
 * @tparam Effect effect type
 */
final case class SttpClient[Effect[_]](
  url: URI,
  method: Option[String],
  webSocket: Boolean,
  system: EffectSystem[Effect],
  backend: SttpBackend[Effect, capabilities.Effect[Effect] with WebSockets]
) extends ClientMessageTransport[Effect, Context] with AutoCloseable with Logging {

  type Capabilities = capabilities.Effect[Effect] with WebSockets

  private val uri = Uri(url)
  private val configuredMethod = method.map(Method.unsafeApply)
  private val webSocketMethod = Method.GET

  override def call(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[ArraySeq.ofByte] = {
    val httpRequest = createHttpRequest(request, mediaType, context)
    system.flatMap(
      system.either(send(httpRequest, request.length)),
      (response: Either[Throwable, Response[Array[Byte]]]) =>
        response.fold(
          error => {
            logger.error("Failed to receive HTTP response", error, Map("URL" -> url))
            system.failed(error)
          },
          message => {
            logger.debug(
              "Received HTTP response",
              Map("URL" -> url, "Status" -> message.code, "Size" -> message.body.length)
            )
            system.pure(Bytes.byteArray.from(message.body))
          }
        )
    )
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[Unit] = {
    val httpRequest = createHttpRequest(request, mediaType, context).response(ignore)
    system.map(send(httpRequest, request.length), (_: Response[Unit]) => ())
  }

  override def defaultContext: Context = SttpClient.defaultContext

  override def close(): Unit = backend.close()

  private def send[R](request: Request[R, Capabilities], size: Int): Effect[Response[R]] = {
    logger.trace("Sending HTTP request", Map("URL" -> url, "Method" -> request.method, "Size" -> size))
    system.flatMap(
      system.either(request.send(backend)),
      (result: Either[Throwable, Response[R]]) =>
        result.fold(
          error => {
            logger.error(
              "Failed to send HTTP request",
              error,
              Map("URL" -> url, "Method" -> request.method, "Size" -> size)
            )
            system.failed(error)
          },
          response => {
            logger.debug("Sent HTTP request", Map("URL" -> url, "Method" -> request.method, "Size" -> size))
            system.pure(response)
          }
        )
    )
  }

  private def sendWebSocket(request: ArraySeq.ofByte): WebSocket[Effect] => Effect[Array[Byte]] =
    webSocket => system.flatMap(webSocket.sendBinary(request.unsafeArray), _ => webSocket.receiveBinary(true))

  private def createHttpRequest(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): Request[Array[Byte], Capabilities] = {
    val contentType = MediaType.unsafeParse(mediaType)
    val properties = context.getOrElse(defaultContext)
    val requestMethod = properties.method.map(Method.unsafeApply).orElse(configuredMethod)
    val requestUrl = properties.url.map(Uri(_)).getOrElse(uri)
    val httpRequest = basicRequest.contentType(contentType).header(Header.accept(contentType))
      .followRedirects(properties.followRedirects).readTimeout(properties.readTimeout)
      .headers(properties.headers.map { case (name, value) => Header(name, value) }: _*)
      .body(request.unsafeArray)
    requestMethod.map(httpMethod => httpRequest.method(httpMethod, requestUrl).response(asByteArrayAlways)).getOrElse {
      httpRequest.method(webSocketMethod, requestUrl).response(asWebSocketAlways(sendWebSocket(request)))
    }
  }
}

case object SttpClient {

  /** Request context type. */
  type Context = HttpProperties[PartialRequest[Either[String, String], Any]]

  implicit val defaultContext: Context = HttpProperties()
}
