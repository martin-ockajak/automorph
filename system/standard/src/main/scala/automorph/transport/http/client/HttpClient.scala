package automorph.transport.http.client

import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.system.{Defer, Deferred}
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.http.{HttpContext, HttpMethod}
import automorph.transport.http.client.HttpClient.{Context, Protocol, Response, Session, WebSocketListener, defaultBuilder}
import automorph.util.Bytes
import automorph.util.Extensions.{EffectOps, TryOps}
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient.Builder
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.WebSocket.Listener
import java.net.http.{HttpRequest, HttpResponse, WebSocket}
import java.nio.ByteBuffer
import java.util.concurrent.{CompletableFuture, CompletionStage}
import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}
import scala.jdk.OptionConverters.RichOptional
import scala.util.Try

/**
 * Standard JRE HttpClient HTTP & WebSocket client message transport plugin.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
 * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
 * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
 * @constructor Creates an HttpClient HTTP & WebSocket message client transport plugin.
 * @param system effect system plugin
 * @param url remote API HTTP or WebSocket URL
 * @param method HTTP request method (default: POST)
 * @param builder HttpClient builder (default: empty)
 * @tparam Effect effect type
 */
final case class HttpClient[Effect[_]] (
  system: EffectSystem[Effect],
  url: URI,
  method: HttpMethod = HttpMethod.Post,
  builder: Builder = defaultBuilder
) extends ClientMessageTransport[Effect, Context] with Logging {

  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
  private val httpEmptyUrl = new URI("http://empty")
  private val webSocketsSchemePrefix = "ws"
  private val httpClient = builder.build
  implicit private val givenSystem: EffectSystem[Effect] = system

  override def call(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    requestContext: Option[Context]
  ): Effect[(ArraySeq.ofByte, Context)] = {
    // Send the request
    createRequest(requestBody, mediaType, requestContext).flatMap { case (request, requestUrl) =>
      val protocol = request.fold(_ => Protocol.Http, _ => Protocol.WebSocket)
      send(request, requestUrl, requestId, protocol).either.flatMap { result =>
        lazy val responseProperties = Map(
          LogProperties.requestId -> requestId,
          "URL" -> requestUrl.toString
        )

        // Process the response
        result.fold(
          error => {
            logger.error(s"Failed to receive $protocol response", error, responseProperties)
            system.failed(error)
          },
          response => {
            val (responseBody, statusCode, _) = response
            logger.debug(s"Received $protocol response", responseProperties ++ statusCode.map("Status" -> _))
            system.pure(responseBody -> responseContext(response))
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
    createRequest(requestBody, mediaType, requestContext).flatMap { case (request, requestUrl) =>
      val protocol = request.fold(_ => Protocol.Http, _ => Protocol.WebSocket)
      send(request, requestUrl, requestId, protocol).map(_ => ())
    }
  }

  override def defaultContext: Context =
    Session.default

  override def close(): Effect[Unit] =
    system.wrap(())

  private def send(
    request: Either[HttpRequest, (Effect[WebSocket], Effect[Response], ArraySeq.ofByte)],
    requestUrl: URI,
    requestId: String,
    protocol: Protocol
  ): Effect[Response] = {
    // Log the request
    lazy val requestProperties = Map(
      LogProperties.requestId -> requestId,
      "URL" -> requestUrl.toString
    ) ++ request.swap.toOption.map("Method" -> _.method)
    logger.trace(s"Sending $protocol request", requestProperties)

    // Send the request
    request.fold(
      // Send HTTP request
      httpRequest =>
        system match {
          case defer: Defer[_] => effect(
              httpClient.sendAsync(httpRequest, BodyHandlers.ofByteArray),
              defer.asInstanceOf[Defer[Effect]]
            ).map(httpResponse)
          case _ => system.wrap(httpResponse(httpClient.send(httpRequest, BodyHandlers.ofByteArray)))
        },
      // Send WebSocket request
      { case (webSocketEffect, resultEffect, requestBody) =>
        withDefer(defer =>
          webSocketEffect.flatMap(webSocket =>
            effect(webSocket.sendBinary(Bytes.byteBuffer.to(requestBody), true), defer).flatMap(_ =>
              resultEffect
            )
          )
        )
      }
    ).either.flatMap(_.fold(
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

  private def createRequest(
    requestBody: ArraySeq.ofByte,
    mediaType: String,
    requestContext: Option[Context]
  ): Effect[(Either[HttpRequest, (Effect[WebSocket], Effect[Response], ArraySeq.ofByte)], URI)] = {
    val httpContext = requestContext.getOrElse(defaultContext)
    val requestUrl = httpContext.base.flatMap(base => Try(base.request.build).toOption).map(_.uri).getOrElse(url)
    requestUrl.getScheme.toLowerCase match {
      case scheme if scheme.startsWith(webSocketsSchemePrefix) =>
        // Create WebSocket request
        withDefer(defer =>
          system.wrap {
            val responseEffect = defer.deferred[Response]
            val response = responseEffect.flatMap(_.effect)
            val webSocketBuilder = createWebSocketBuilder(httpContext)
            val webSocket = prepareWebSocket(webSocketBuilder, requestUrl, responseEffect, defer)
            Right((webSocket, response, requestBody)) -> requestUrl
          }
        )
      case _ =>
        // Create HTTP request
        system.wrap {
          val httpRequest = createHttpRequest(requestBody, requestUrl, mediaType, httpContext)
          Left(httpRequest) -> httpRequest.uri
        }
    }
  }

  private def createHttpRequest(
    requestBody: ArraySeq.ofByte,
    requestUrl: URI,
    mediaType: String,
    httpContext: Context
  ): HttpRequest = {
    val baseBuilder = httpContext.base.map(_.request).getOrElse(HttpRequest.newBuilder)
    val baseRequest = Try(baseBuilder.build).toOption
    val requestMethod = httpContext.method.map(_.name).getOrElse(method.name)
    require(httpMethods.contains(requestMethod), s"Invalid HTTP method: $requestMethod")
    val headers = httpContext.headers.map { case (name, value) => Seq(name, value) }.flatten
    val headersBuilder = baseBuilder.uri(requestUrl)
      .method(requestMethod, BodyPublishers.ofByteArray(requestBody.unsafeArray))
    val requestBuilder = (headers match {
      case Seq() => headersBuilder
      case values => headersBuilder.headers(values.toArray*)
    }).header(contentTypeHeader, mediaType)
      .header(acceptHeader, mediaType)
    httpContext.readTimeout.map { timeout =>
      java.time.Duration.ofMillis(timeout.toMillis)
    }.orElse(baseRequest.flatMap(_.timeout.toScala)).map { timeout =>
      requestBuilder.timeout(timeout)
    }.getOrElse(requestBuilder).build
  }

  private def prepareWebSocket(
    builder: WebSocket.Builder,
    requestUrl: URI,
    responseEffect: Effect[Deferred[Effect, Response]],
    defer: Defer[Effect]
  ): Effect[WebSocket] =
    responseEffect.flatMap(response =>
      effect(
        builder.buildAsync(
          requestUrl,
          WebSocketListener(
            requestUrl,
            response,
            system
          )
        ),
        defer
      )
    )

  private def createWebSocketBuilder(httpContext: Context): WebSocket.Builder = {
    val baseBuilder = httpContext.base.map(_.request).getOrElse(HttpRequest.newBuilder)
    val webSocketHeaders =
      baseBuilder.uri(httpEmptyUrl).build.headers.map.asScala.toSeq.flatMap { case (name, values) =>
        values.asScala.map(name -> _)
      } ++ httpContext.headers
    val connectionBuilder = httpClient.connectTimeout.toScala
      .map(httpClient.newWebSocketBuilder.connectTimeout)
      .getOrElse(httpClient.newWebSocketBuilder)
    val headersBuilder = LazyList.iterate(connectionBuilder -> webSocketHeaders) { case (builder, headers) =>
      headers.headOption.map { case (name, value) =>
        builder.header(name, value) -> headers.tail
      }.getOrElse(builder -> headers)
    }.dropWhile(!_._2.isEmpty).headOption.map(_._1).getOrElse(connectionBuilder)
    headersBuilder
  }

  private def responseContext(response: Response): Context = {
    val (_, statusCode, headers) = response
    statusCode.map(defaultContext.statusCode).getOrElse(defaultContext).headers(headers*)
  }

  private def httpResponse(response: HttpResponse[Array[Byte]]): Response = {
    val headers = response.headers.map.asScala.toSeq.flatMap { case (name, values) =>
      values.asScala.map(name -> _)
    }
    (Bytes.byteArray.from(response.body), Some(response.statusCode), headers)
  }

  private def effect[T](
    completableFuture: => CompletableFuture[T],
    defer: Defer[Effect]
  ): Effect[T] =
    defer.deferred[T].flatMap { deferred =>
      Try(completableFuture).pureFold(
        error => deferred.fail(error).run,
        (value: CompletableFuture[T]) => {
          value.handle { case (result, exception) =>
            Option(result).map { value =>
              deferred.succeed(value).run
            }.getOrElse {
              val error = Option(exception).getOrElse {
                new IllegalStateException("Missing completable future result")
              }
              deferred.fail(error).run
            }
          }
          ()
        }
      )
      deferred.effect
    }

  private def withDefer[T](function: Defer[Effect] => Effect[T]): Effect[T] = system match {
    case defer: Defer[_] => function(defer.asInstanceOf[Defer[Effect]])
    case _ => system.failed(new IllegalArgumentException(
        s"Effect system without deferred effect support cannot be used with WebSocket: ${system.getClass.getName}"
      ))
  }
}

object HttpClient {

  /** Request context type. */
  type Context = HttpContext[Session]

  val defaultBuilder = java.net.http.HttpClient.newBuilder

  private case class WebSocketListener[Effect[_]](
    url: URI,
    response: Deferred[Effect, Response],
    system: EffectSystem[Effect]
  ) extends Listener {

    private val buffers = ArrayBuffer.empty[ArraySeq.ofByte]

    override def onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage[_] = {
      buffers += Bytes.byteBuffer.from(data)
      if (last) {
        val outputStream = new ByteArrayOutputStream(buffers.map(_.length).sum)
        buffers.foreach(buffer => outputStream.write(buffer.unsafeArray, 0, buffer.length))
        buffers.clear()
        val responseBody = Bytes.byteArray.from(outputStream.toByteArray)
        system.run(response.succeed((responseBody, None, Seq())).asInstanceOf[Effect[Any]])
      }
      super.onBinary(webSocket, data, last)
    }

    override def onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage[_] =
      super.onClose(webSocket, statusCode, reason)

    override def onError(webSocket: WebSocket, error: Throwable): Unit = {
      response.fail(error)
      super.onError(webSocket, error)
    }
  }

  private type Response = (ArraySeq.ofByte, Option[Int], Seq[(String, String)])

  /** Transport protocol. */
  sealed abstract private class Protocol(val name: String) {
    override def toString: String = name
  }

  /** Transport protocols. */
  private object Protocol {

    case object Http extends Protocol("HTTP")

    case object WebSocket extends Protocol("WebSocket")
  }

  final case class Session(request: HttpRequest.Builder)

  object Session {
    /** Implicit default context value. */
    implicit val default: HttpContext[Session] = HttpContext()
  }
}
