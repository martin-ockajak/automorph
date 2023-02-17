package automorph.transport.http.client

import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.EffectSystem
import automorph.spi.system.{Completable, CompletableEffectSystem}
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.http.client.HttpClient.{Context, Session, defaultBuilder}
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{ByteArrayOps, ByteBufferOps, EffectOps, InputStreamOps, TryOps}
import java.io.{ByteArrayOutputStream, InputStream}
import java.net.URI
import java.net.http.HttpClient.Builder
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.WebSocket.Listener
import java.net.http.{HttpRequest, HttpResponse, WebSocket}
import java.nio.ByteBuffer
import java.util.concurrent.{CompletableFuture, CompletionStage}
import scala.collection.immutable.ListMap
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}
import scala.jdk.OptionConverters.RichOptional
import scala.util.Try

/**
 * Standard JRE HttpClient HTTP & WebSocket client message transport plugin.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
 * @see
 *   [[https://openjdk.org/groups/net/httpclient/intro.html documentation]]
 * @see
 *   [[https://docs.oracle.com/en/java/javase/19/docs/api/java.net.http/java/net/http/HttpClient.html API]]
 * @constructor
 *   Creates an HttpClient HTTP & WebSocket message client transport plugin.
 * @param system
 *   effect system plugin
 * @param url
 *   remote API HTTP or WebSocket URL
 * @param method
 *   HTTP request method (default: POST)
 * @param builder
 *   HttpClient builder (default: empty)
 * @tparam Effect
 *   effect type
 */
final case class HttpClient[Effect[_]](
  system: EffectSystem[Effect],
  url: URI,
  method: HttpMethod = HttpMethod.Post,
  builder: Builder = defaultBuilder,
) extends ClientMessageTransport[Effect, Context] with Logging {

  private type Response = (InputStream, Option[Int], Seq[(String, String)])

  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = HttpMethod.values.map(_.name).toSet
  private val httpEmptyUrl = new URI("http://empty")
  private val webSocketsSchemePrefix = "ws"
  private val httpClient = builder.build
  private val log = MessageLog(logger, Protocol.Http.name)
  implicit private val givenSystem: EffectSystem[Effect] = system

  override def call(
    requestBody: InputStream,
    requestContext: Option[Context],
    requestId: String,
    mediaType: String,
  ): Effect[(InputStream, Context)] =
    // Send the request
    createRequest(requestBody, mediaType, requestContext).flatMap { case (request, requestUrl) =>
      val protocol = request.fold(_ => Protocol.Http, _ => Protocol.WebSocket)
      send(request, requestUrl, requestId, protocol).either.flatMap { result =>
        lazy val responseProperties = ListMap(LogProperties.requestId -> requestId, "URL" -> requestUrl.toString)

        // Process the response
        result.fold(
          error => {
            log.failedReceiveResponse(error, responseProperties, protocol.name)
            system.failed(error)
          },
          response => {
            val (responseBody, statusCode, _) = response
            log.receivedResponse(responseProperties ++ statusCode.map("Status" -> _.toString), protocol.name)
            system.pure(responseBody -> getResponseContext(response))
          },
        )
      }
    }

  private def getResponseContext(response: Response): Context = {
    val (_, statusCode, headers) = response
    statusCode.map(defaultContext.statusCode).getOrElse(defaultContext).headers(headers*)
  }

  override def message(
    requestBody: InputStream,
    requestContext: Option[Context],
    requestId: String,
    mediaType: String,
  ): Effect[Unit] =
    createRequest(requestBody, mediaType, requestContext).flatMap { case (request, requestUrl) =>
      val protocol = request.fold(_ => Protocol.Http, _ => Protocol.WebSocket)
      send(request, requestUrl, requestId, protocol).map(_ => ())
    }

  private def send(
    request: Either[HttpRequest, (Effect[WebSocket], Effect[Response], InputStream)],
    requestUrl: URI,
    requestId: String,
    protocol: Protocol,
  ): Effect[Response] =
    log(
      requestId,
      requestUrl,
      request.swap.toOption.map(_.method),
      protocol,
      request.fold(
        // Send HTTP request
        httpRequest =>
          system match {
            case completableSystem: CompletableEffectSystem[?] =>
              effect(
                httpClient.sendAsync(httpRequest, BodyHandlers.ofByteArray),
                completableSystem.asInstanceOf[CompletableEffectSystem[Effect]],
              )
                .map(httpResponse)
            case _ => system.wrap(httpResponse(httpClient.send(httpRequest, BodyHandlers.ofByteArray)))
          },
        // Send WebSocket request
        { case (webSocketEffect, resultEffect, requestBody) =>
          withCompletable(completableSystem =>
            webSocketEffect.flatMap(webSocket =>
              effect(webSocket.sendBinary(requestBody.toByteBuffer, true), completableSystem).flatMap(_ => resultEffect)
            )
          )
        },
      ),
    )

  private def log(
    requestId: String,
    requestUrl: URI,
    requestMethod: Option[String],
    protocol: Protocol,
    response: => Effect[Response],
  ): Effect[Response] = {
    lazy val requestProperties = ListMap(LogProperties.requestId -> requestId, "URL" -> requestUrl.toString) ++
      requestMethod.map("Method" -> _)
    log.sendingRequest(requestProperties, protocol.name)
    response.either.flatMap(
      _.fold(
        error => {
          log.failedSendRequest(error, requestProperties, protocol.name)
          system.failed(error)
        },
        response => {
          log.sentRequest(requestProperties, protocol.name)
          system.pure(response)
        },
      )
    )
  }

  private def httpResponse(response: HttpResponse[Array[Byte]]): Response = {
    val headers = response.headers.map.asScala.toSeq.flatMap { case (name, values) => values.asScala.map(name -> _) }
    (response.body.toInputStream, Some(response.statusCode), headers)
  }

  private def withCompletable[T](function: CompletableEffectSystem[Effect] => Effect[T]): Effect[T] =
    system match {
      case completableSystem: CompletableEffectSystem[?] =>
        function(completableSystem.asInstanceOf[CompletableEffectSystem[Effect]])
      case _ => system.failed(new IllegalArgumentException(
          s"""WebSocket protocol not available for effect system
            | not supporting completable effects: ${system.getClass.getName}""".stripMargin
        ))
    }

  private def effect[T](
    completableFuture: => CompletableFuture[T],
    completableSystem: CompletableEffectSystem[Effect],
  ): Effect[T] =
    completableSystem.completable[T].flatMap { completable =>
      Try(completableFuture).pureFold(
        exception => completable.fail(exception).run,
        value => {
          value.handle { case (result, error) =>
            Option(result).map(value => completable.succeed(value).run).getOrElse {
              completable.fail(Option(error).getOrElse(new IllegalStateException("Missing completable future result")))
                .run
            }
          }
          ()
        },
      )
      completable.effect
    }

  private def createRequest(
    requestBody: InputStream,
    mediaType: String,
    requestContext: Option[Context],
  ): Effect[(Either[HttpRequest, (Effect[WebSocket], Effect[Response], InputStream)], URI)] = {
    val httpContext = requestContext.getOrElse(defaultContext)
    val requestUrl = httpContext.overrideUrl {
      httpContext.transport.flatMap(transport => Try(transport.request.build).toOption).map(_.uri).getOrElse(url)
    }
    requestUrl.getScheme.toLowerCase match {
      case scheme if scheme.startsWith(webSocketsSchemePrefix) =>
        // Create WebSocket request
        withCompletable(completableSystem =>
          system.wrap {
            val completableResponse = completableSystem.completable[Response]
            val response = completableResponse.flatMap(_.effect)
            val webSocketBuilder = createWebSocketBuilder(httpContext)
            val webSocket = connectWebSocket(webSocketBuilder, requestUrl, completableResponse, completableSystem)
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

  override def defaultContext: Context =
    Session.defaultContext

  private def createHttpRequest(
    requestBody: InputStream,
    requestUrl: URI,
    mediaType: String,
    httpContext: Context,
  ): HttpRequest = {
    // Method & body
    val transportBuilder = httpContext.transport.map(_.request).getOrElse(HttpRequest.newBuilder)
    val transportRequest = Try(transportBuilder.build).toOption
    val requestMethod = httpContext.method.map(_.name).getOrElse(method.name)
    require(httpMethods.contains(requestMethod), s"Invalid HTTP method: $requestMethod")
    val methodBuilder = transportBuilder.uri(requestUrl)
      .method(requestMethod, BodyPublishers.ofByteArray(requestBody.toArray))

    // Headers
    val headers = httpContext.headers.flatMap { case (name, value) => Seq(name, value) }
    val headersBuilder = (headers match {
      case Seq() => methodBuilder
      case values => methodBuilder.headers(values.toArray*)
    }).header(contentTypeHeader, mediaType).header(acceptHeader, mediaType)

    // Timeout
    httpContext.timeout.map(timeout => java.time.Duration.ofMillis(timeout.toMillis))
      .orElse(transportRequest.flatMap(_.timeout.toScala)).map(timeout => headersBuilder.timeout(timeout))
      .getOrElse(headersBuilder).build
  }

  private def connectWebSocket(
    builder: WebSocket.Builder,
    requestUrl: URI,
    responseEffect: Effect[Completable[Effect, Response]],
    completableSystem: CompletableEffectSystem[Effect],
  ): Effect[WebSocket] =
    responseEffect.flatMap(response =>
      effect(builder.buildAsync(requestUrl, webSocketListener(response)), completableSystem)
    )

  private def webSocketListener(response: Completable[Effect, Response]): Listener =
    new Listener {

      private val buffers = ArrayBuffer.empty[Array[Byte]]

      override def onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage[?] = {
        buffers += data.toArray
        if (last) {
          val outputStream = new ByteArrayOutputStream(buffers.map(_.length).sum)
          buffers.foreach(buffer => outputStream.write(buffer, 0, buffer.length))
          buffers.clear()
          val responseBody = outputStream.toByteArray.toInputStream
          response.succeed((responseBody, None, Seq())).run
        }
        super.onBinary(webSocket, data, last)
      }

      override def onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage[?] =
        super.onClose(webSocket, statusCode, reason)

      override def onError(webSocket: WebSocket, error: Throwable): Unit = {
        response.fail(error)
        super.onError(webSocket, error)
      }
    }

  private def createWebSocketBuilder(httpContext: Context): WebSocket.Builder = {
    // Headers
    val transportBuilder = httpContext.transport.map(_.request).getOrElse(HttpRequest.newBuilder)
    val headers = transportBuilder.uri(httpEmptyUrl).build.headers.map.asScala.toSeq.flatMap { case (name, values) =>
      values.asScala.map(name -> _)
    } ++ httpContext.headers
    val headersBuilder = LazyList.iterate(httpClient.newWebSocketBuilder -> headers) { case (builder, headers) =>
      headers.headOption.map { case (name, value) => builder.header(name, value) -> headers.tail }
        .getOrElse(builder -> headers)
    }.dropWhile(_._2.nonEmpty).headOption.map(_._1).getOrElse(httpClient.newWebSocketBuilder)

    // Timeout
    httpClient.connectTimeout.toScala.map(headersBuilder.connectTimeout).getOrElse(headersBuilder)
  }

  override def close(): Effect[Unit] =
    system.wrap(())
}

object HttpClient {

  /** Request context type. */
  type Context = HttpContext[Session]

  /** Default HTTP client builder. */
  val defaultBuilder: Builder = java.net.http.HttpClient.newBuilder

  final case class Session(request: HttpRequest.Builder)

  object Session {

    /** Implicit default context value. */
    implicit val defaultContext: HttpContext[Session] = HttpContext()
  }
}
