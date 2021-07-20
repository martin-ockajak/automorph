package automorph.transport.http.client

import automorph.log.Logging
import automorph.spi.{ClientMessageTransport, EffectSystem}
import automorph.transport.http.HttpProperties
import automorph.transport.http.client.SttpClient.{Context, WebSocket}
import automorph.util.Bytes
import java.net.URI
import scala.collection.immutable.ArraySeq
import sttp.capabilities.WebSockets
import sttp.client3.{PartialRequest, Request, Response, SttpBackend, asByteArrayAlways, asWebSocketAlways, basicRequest, ignore}
import sttp.model.{Header, MediaType, Method, Uri}

/**
 * STTP client transport plugin using HTTP as message transport protocol with the specified STTP backend.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see [[https://sttp.softwaremill.com/en/latest/ Documentation]]
 * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
 * @constructor Creates an STTP client transport plugin with the specified STTP backend.
 * @param url endpoint URL
 * @param method HTTP method
 * @param system effect system plugin
 * @param method HTTP method
 * @param backend STTP backend
 * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
 * @tparam Effect effect type
 */
final case class SttpClient[Effect[_]](
  url: URI,
  method: String,
  system: EffectSystem[Effect],
  backend: SttpBackend[Effect, WebSocket[Effect]],
  webSocket: Boolean = false
) extends ClientMessageTransport[Effect, Context] with AutoCloseable with Logging {

  private val defaultUrl = Uri(url)
  private val defaultMethod = Method.unsafeApply(method)
  private val protocol = if (webSocket) "WebSocket" else "HTTP"

  override def call(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[ArraySeq.ofByte] = {
    val httpRequest = createHttpRequest(request, mediaType, context)
    system.flatMap(
      system.either(send(httpRequest, request)),
      (response: Either[Throwable, Response[Array[Byte]]]) =>
        response.fold(
          error => {
            logger.error(s"Failed to receive $protocol response", error, Map("URL" -> url))
            system.failed(error)
          },
          message => {
            logger.debug(
              s"Received $protocol response",
              Map("URL" -> url, "Status" -> message.code, "Size" -> message.body.length)
            )
            system.pure(Bytes.byteArray.from(message.body))
          }
        )
    )
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[Unit] = {
    val httpRequest = createHttpRequest(request, mediaType, context).response(ignore)
    system.map(send(httpRequest, request), (_: Response[Unit]) => ())
  }

  override def defaultContext: Context = SttpClient.defaultContext

  override def close(): Unit = backend.close()

  private def send[R](httpRequest: Request[R, WebSocket[Effect]], request: ArraySeq.ofByte): Effect[Response[R]] = {
    logger.trace(
      s"Sending $protocol httpRequest",
      Map("URL" -> url, "Method" -> httpRequest.method, "Size" -> request.size)
    )
    system.flatMap(
      system.either(httpRequest.send(backend)),
      (result: Either[Throwable, Response[R]]) =>
        result.fold(
          error => {
            logger.error(
              s"Failed to send $protocol httpRequest",
              error,
              Map("URL" -> url, "Method" -> httpRequest.method, "Size" -> request.size)
            )
            system.failed(error)
          },
          response => {
            logger.debug(
              s"Sent $protocol httpRequest",
              Map("URL" -> url, "Method" -> httpRequest.method, "Size" -> request.size)
            )
            system.pure(response)
          }
        )
    )
  }

  private def sendWebSocket(request: ArraySeq.ofByte): sttp.ws.WebSocket[Effect] => Effect[Array[Byte]] =
    webSocket => system.flatMap(webSocket.sendBinary(request.unsafeArray), _ => webSocket.receiveBinary(true))

  private def createHttpRequest(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): Request[Array[Byte], WebSocket[Effect]] = {
    val contentType = MediaType.unsafeParse(mediaType)
    val properties = context.getOrElse(defaultContext)
    val requestMethod = properties.method.map(Method.unsafeApply).getOrElse(defaultMethod)
    val requestUrl = properties.url.map(Uri(_)).getOrElse(defaultUrl)
    val httpRequest = basicRequest.method(requestMethod, requestUrl)
      .contentType(contentType).header(Header.accept(contentType))
      .followRedirects(properties.followRedirects).readTimeout(properties.readTimeout)
      .headers(properties.headers.map { case (name, value) => Header(name, value) }: _*)
    if (webSocket) {
      httpRequest.response(asWebSocketAlways(sendWebSocket(request)))
    } else {
      httpRequest.body(request.unsafeArray).response(asByteArrayAlways)
    }
  }
}

case object SttpClient {

  /** STTP backend WebSocket capabilities type. */
  type WebSocket[Effect[_]] = sttp.capabilities.Effect[Effect] with WebSockets

  /** Request context type. */
  type Context = HttpProperties[PartialRequest[Either[String, String], Any]]

  implicit val defaultContext: Context = HttpProperties()
}
