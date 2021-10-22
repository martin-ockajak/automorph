package automorph.transport.http.client

import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.client.HttpClient.{defaultBuilder, Context, Protocol, Response, WebSocketListener}
import automorph.util.Bytes
import automorph.util.Extensions.TryOps
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.WebSocket.Listener
import java.net.http.{HttpRequest, HttpResponse, WebSocket}
import java.net.{HttpURLConnection, URI}
import java.nio.ByteBuffer
import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function.BiFunction
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}
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
 * @param promisedEffect creates a not yet completed effect and its completion and failure functions
 * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
 * @param builder HttpClient builder
 * @tparam Effect effect type
 */
final case class HttpClient[Effect[_]](
  url: URI,
  method: String,
  system: EffectSystem[Effect],
  promisedEffect: () => (Effect[Any], Any => Unit, Throwable => Unit),
  webSocket: Boolean = false,
  builder: java.net.http.HttpClient.Builder = defaultBuilder
) extends ClientMessageTransport[Effect, Context] with Logging {

  private val httpClient = builder.build
  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
  private val protocol = if (webSocket) Protocol.WebSocket else Protocol.Http
  private val webSockets = TrieMap[URI, WebSocket]()
  require(httpMethods.contains(method), s"Invalid HTTP method: $method")

  override def call(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    context: Option[Context]
  ): Effect[ArraySeq.ofByte] = {
    val httpRequest = createHttpRequest(requestBody, mediaType, context)
    system.flatMap(
      system.either(send(Left(httpRequest), httpRequest.uri, requestId)),
      (result: Either[Throwable, Response]) => {
        lazy val responseProperties = Map(
          LogProperties.requestId -> requestId,
          "URL" -> httpRequest.uri.toString
        )
        result.fold(
          error => {
            logger.error(s"Failed to receive $protocol response", error, responseProperties)
            system.failed(error)
          },
          { case (responseBody, statusCode) =>
            logger.debug(s"Received $protocol response", responseProperties ++ statusCode.map("Status" -> _))
            system.pure(responseBody)
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
    val httpRequest = createHttpRequest(requestBody, mediaType, context)
    system.map(send(Left(httpRequest), httpRequest.uri, requestId), (_: Response) => ())
  }

  override def defaultContext: Context = HttpContext.default

  override def close(): Effect[Unit] = system.wrap(())

  private def send(
    request: Either[HttpRequest, (WebSocket.Builder, ArraySeq.ofByte)],
    requestUrl: URI,
    requestId: String
  ): Effect[Response] = {
    // Log the request
    lazy val requestProperties = Map(
      LogProperties.requestId -> requestId,
      "URL" -> requestUrl.toString
    ) ++ request.swap.toOption.map("Method" -> _.method)
    logger.trace(s"Sending $protocol httpRequest", requestProperties)

    // Send the request
    system.flatMap(
      system.either(request.fold(
        // Use HTTP connection
        httpRequest =>
          system.map(
            effect(httpClient.sendAsync(httpRequest, BodyHandlers.ofByteArray)),
            response => Bytes.byteArray.from(response.body) -> Some(response.statusCode)
          ),

        // Use WebSocket connection
        { case (webSocketBuilder, requestBody) =>
          val (effectResult, completeEffect, failEFfect) = promisedEffect()
          val listener = WebSocketListener(completeEffect.asInstanceOf[Response => Unit], failEFfect)
          system.flatMap(
            effect(webSocketBuilder.buildAsync(url, listener)),
            webSocket =>
              system.flatMap(
                effect(webSocket.sendBinary(Bytes.byteBuffer.to(requestBody), true)),
                _ => effectResult.asInstanceOf[Effect[Response]]
              )
          )
        }
      )),
      (result: Either[Throwable, Response]) =>
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

  private def createHttpRequest(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): HttpRequest = {
    val http = context.getOrElse(defaultContext)
    val baseBuilder = http.base.map(_.request).getOrElse(HttpRequest.newBuilder)
    val baseRequest = Try(baseBuilder.build).toOption
    val requestUrl = http.overrideUrl(baseRequest.map(_.uri).getOrElse(url))
    val requestMethod = http.method.getOrElse(method)
    require(httpMethods.contains(requestMethod), s"Invalid HTTP method: $requestMethod")
    val headers = http.headers.map { case (name, value) => Seq(name, value) }.flatten.toArray
    val httpRequestBuilder = baseBuilder.uri(requestUrl)
      .method(requestMethod, BodyPublishers.ofByteArray(request.unsafeArray))
      .header(contentTypeHeader, mediaType)
      .header(acceptHeader, mediaType)
      .headers(headers*)
    http.readTimeout.map { timeout =>
      java.time.Duration.ofMillis(timeout.toMillis)
    }.orElse(baseRequest.flatMap(_.timeout.toScala)).map { timeout =>
      httpRequestBuilder.timeout(timeout)
    }.getOrElse(httpRequestBuilder).build
  }

  private def createWebSocketBuilder(context: Option[Context]): (WebSocket.Builder, URI) = {
    val http = context.getOrElse(defaultContext)
    val baseBuilder = http.base.map(_.request).getOrElse(HttpRequest.newBuilder)
    val baseRequest = Try(baseBuilder.build).toOption
    val requestUrl = http.overrideUrl(baseRequest.map(_.uri).getOrElse(url))
    val httpHeaders = baseBuilder.uri(requestUrl).build.headers.map.asScala.toSeq.flatMap { case (name, values) =>
      values.asScala.map(name -> _)
    } ++ http.headers
    val connectionBuilder = httpClient.connectTimeout.toScala
      .map(httpClient.newWebSocketBuilder.connectTimeout)
      .getOrElse(httpClient.newWebSocketBuilder)
    val headersBuilder = LazyList.iterate(connectionBuilder -> httpHeaders) { case (builder, headers) =>
      headers.headOption.map { case (name, value) =>
        builder.header(name, value) -> headers.tail
      }.getOrElse(builder -> headers)
    }.dropWhile(!_._2.isEmpty).headOption.map(_._1).getOrElse(connectionBuilder)
    headersBuilder -> requestUrl
  }

  private def effect[T](completableFuture: => CompletableFuture[T]): Effect[T] = {
    val (effectResult, completeEffect, failEffect) = promisedEffect()
    Try(completableFuture).pureFold(
      error => failEffect(error),
      (value: CompletableFuture[T]) =>
        value.handle { case (result, exception) =>
          Option(result).map(completeEffect).orElse(Option(exception).map(failEffect)).getOrElse(failEffect)
        }
    )
    effectResult.asInstanceOf[Effect[T]]
  }
}

object HttpClient {

  /** Request context type. */
  type Context = Http[HttpContext]

  /** Response type. */
  type Response = (ArraySeq.ofByte, Option[Int])

  val defaultBuilder = java.net.http.HttpClient.newBuilder

  private case class WebSocketListener(
    completeEffect: Response => Unit,
    failEffect: Throwable => Unit
  ) extends Listener {

    override def onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage[_] = {
      completeEffect(Bytes.byteBuffer.from(data) -> None)
      super.onBinary(webSocket, data, last)
    }

    override def onError(webSocket: WebSocket, error: Throwable): Unit = {
      failEffect(error)
      super.onError(webSocket, error)
    }
  }

  /** Transport protocol. */
  sealed abstract private class Protocol(val name: String) {
    override def toString: String = name
  }

  /** Transport protocols. */
  private object Protocol {

    case object Http extends Protocol("HTTP")

    case object WebSocket extends Protocol("WebSocket")
  }
}

final case class HttpContext(request: HttpRequest.Builder)

object HttpContext {
  /** Implicit default context value. */
  implicit val default: Http[HttpContext] = Http()
}
