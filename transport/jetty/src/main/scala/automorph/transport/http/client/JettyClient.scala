package automorph.transport.http.client

import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.EffectSystem
import automorph.spi.system.{Defer, Deferred}
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.http.client.JettyClient.{Context, Session, defaultClient}
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{ByteArrayOps, EffectOps, InputStreamOps, TryOps}
import java.io.InputStream
import java.net.URI
import java.util
import java.util.concurrent.{CompletableFuture, TimeUnit}
import org.eclipse.jetty.client.api.{Request, Result}
import org.eclipse.jetty.client.util.{BufferingResponseListener, BytesRequestContent}
import org.eclipse.jetty.client.{HttpClient, api}
import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.websocket.api.{WebSocketListener, WriteCallback}
import org.eclipse.jetty.websocket.client.{ClientUpgradeRequest, WebSocketClient}
import org.eclipse.jetty.{http, websocket}
import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.{IterableHasAsScala, SeqHasAsJava}
import scala.util.Try

/**
 * Jetty HTTP & WebSocket client message transport plugin.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
 * @see [[https://jetty.io Library documentation]]
 * @see [[https://www.javadoc.io/doc/io.jetty/jetty-core/latest/index.html API]]
 * @constructor Creates an Jetty HTTP & WebSocket message client transport plugin.
 * @param system effect system plugin
 * @param url remote API HTTP or WebSocket URL
 * @param method HTTP request method (default: POST)
 * @param httpClient Jetty HTTP client
 * @tparam Effect effect type
 */
final case class JettyClient[Effect[_]](
  system: EffectSystem[Effect],
  url: URI,
  method: HttpMethod = HttpMethod.Post,
  httpClient: HttpClient = defaultClient
) extends ClientMessageTransport[Effect, Context] with Logging {

  private type Response = (InputStream, Option[Int], Seq[(String, String)])

  private val webSocketsSchemePrefix = "ws"
  private val webSocketClient = new WebSocketClient(httpClient)
  private val log = MessageLog(logger, Protocol.Http.name)
  implicit private val givenSystem: EffectSystem[Effect] = system
  if (!httpClient.isStarted) {
    httpClient.start()
  }
  webSocketClient.start()

  override def call(
    requestBody: InputStream,
    requestContext: Option[Context],
    requestId: String,
    mediaType: String
  ): Effect[(InputStream, Context)] = {
    // Send the request
    createRequest(requestBody, mediaType, requestContext).flatMap { case (request, requestUrl) =>
      val protocol = request.fold(_ => Protocol.Http, _ => Protocol.WebSocket)
      send(request, requestUrl, requestId, protocol).either.flatMap { result =>
        lazy val responseProperties = ListMap(
          LogProperties.requestId -> requestId,
          "URL" -> requestUrl.toString
        )

        // Process the response
        result.fold(
          error => {
            log.failedReceiveResponse(error, responseProperties, protocol.name)
            system.failed(error)
          },
          response => {
            val (responseBody, statusCode, _) = response
            log.receivedResponse(responseProperties ++ statusCode.map("Status" -> _.toString), protocol.name)
            system.pure(responseBody -> responseContext(response))
          }
        )
      }
    }
  }

  override def message(
    requestBody: InputStream,
    requestContext: Option[Context],
    requestId: String,
    mediaType: String
  ): Effect[Unit] = {
    createRequest(requestBody, mediaType, requestContext).flatMap { case (request, requestUrl) =>
      val protocol = request.fold(_ => Protocol.Http, _ => Protocol.WebSocket)
      send(request, requestUrl, requestId, protocol).map(_ => ())
    }
  }

  override def defaultContext: Context =
    Session.defaultContext

  override def close(): Effect[Unit] =
    system.wrap {
      webSocketClient.stop()
      httpClient.stop()
    }

  private def send(
    request: Either[Request, (Effect[websocket.api.Session], Effect[Response], InputStream)],
    requestUrl: URI,
    requestId: String,
    protocol: Protocol
  ): Effect[Response] = {
    log(
      requestId,
      requestUrl,
      request.swap.toOption.map(_.getMethod),
      protocol,
      request.fold(
        // Send HTTP request
        httpRequest =>
          system match {
            case defer: Defer[?] =>
              defer.asInstanceOf[Defer[Effect]].deferred[Response].flatMap { deferredResponse =>
                val responseListener = new BufferingResponseListener {

                  override def onComplete(result: Result): Unit = {
                    Option(result.getResponseFailure).map(error => deferredResponse.fail(error).run).getOrElse {
                      deferredResponse.succeed(httpResponse(result.getResponse, getContent)).run
                    }
                  }
                }
                httpRequest.send(responseListener)
                deferredResponse.effect
              }
            case _ => system.wrap(httpRequest.send()).map(response => httpResponse(response, response.getContent))
          },
        // Send WebSocket request
        { case (webSocketEffect, resultEffect, requestBody) =>
          withDefer(defer =>
            defer.deferred[Unit].flatMap { deferredSent =>
              webSocketEffect.flatMap { webSocket =>
                webSocket.getRemote.sendBytes(
                  requestBody.toByteBuffer,
                  new WriteCallback {
                    override def writeSuccess(): Unit =
                      deferredSent.succeed(()).run

                    override def writeFailed(error: Throwable): Unit =
                      deferredSent.fail(error).run
                  }
                )
                deferredSent.effect.flatMap(_ => resultEffect)
              }
            }
          )
        }
      )
    )
  }

  private def log(
    requestId: String,
    requestUrl: URI,
    requestMethod: Option[String],
    protocol: Protocol,
    response: => Effect[Response]
  ): Effect[Response] = {
    lazy val requestProperties = ListMap(
      LogProperties.requestId -> requestId,
      "URL" -> requestUrl.toString
    ) ++ requestMethod.map("Method" -> _)
    log.sendingRequest(requestProperties, protocol.name)
    response.either.flatMap(_.fold(
      error => {
        log.failedSendRequest(error, requestProperties, protocol.name)
        system.failed(error)
      },
      response => {
        log.sentRequest(requestProperties, protocol.name)
        system.pure(response)
      }
    ))
  }

  private def createRequest(
    requestBody: InputStream,
    mediaType: String,
    requestContext: Option[Context]
  ): Effect[(Either[Request, (Effect[websocket.api.Session], Effect[Response], InputStream)], URI)] = {
    val httpContext = requestContext.getOrElse(defaultContext)
    val requestUrl = httpContext.overrideUrl {
      httpContext.transport.map(transport => transport.request.getURI).getOrElse(url)
    }
    requestUrl.getScheme.toLowerCase match {
      case scheme if scheme.startsWith(webSocketsSchemePrefix) =>
        // Create WebSocket request
        withDefer(defer =>
          system.wrap {
            val deferredResponse = defer.deferred[Response]
            val response = deferredResponse.flatMap(_.effect)
            val upgradeRequest = createWebSocketRequest(httpContext, requestUrl)
            val webSocket = connectWebSocket(upgradeRequest, requestUrl, deferredResponse, defer)
            Right((webSocket, response, requestBody)) -> requestUrl
          }
        )
      case _ =>
        // Create HTTP request
        system.wrap {
          val httpRequest = createHttpRequest(requestBody, requestUrl, mediaType, httpContext)
          Left(httpRequest) -> httpRequest.getURI
        }
    }
  }

  private def createHttpRequest(
    requestBody: InputStream,
    requestUrl: URI,
    mediaType: String,
    httpContext: Context
  ): Request = {
    // URL, method & body
    val requestMethod = http.HttpMethod.valueOf(httpContext.method.orElse {
      httpContext.transport.map(_.request.getMethod).map(HttpMethod.valueOf)
    }.getOrElse(method).name)
    val transportRequest = httpContext.transport.map(_.request).getOrElse(httpClient.newRequest(requestUrl))
    val bodyRequest = transportRequest.method(requestMethod).body(new BytesRequestContent(requestBody.toArray))

    // Headers
    val headersRequest = bodyRequest.headers(httpFields => {
      httpContext.headers.foreach { case (name, value) =>
        httpFields.add(name, value)
      }
      httpFields.put(HttpHeader.CONTENT_TYPE, mediaType)
      httpFields.put(HttpHeader.ACCEPT, mediaType)
      ()
    })

    // Timeout & follow redirects
    val timeoutRequest = httpContext.timeout.map { timeout =>
      headersRequest.timeout(timeout.toMillis, TimeUnit.MILLISECONDS)
    }.getOrElse(headersRequest)
    httpContext.followRedirects.map { followRedirects =>
      timeoutRequest.followRedirects(followRedirects)
    }.getOrElse(timeoutRequest)
  }

  private def connectWebSocket(
    upgradeRequest: ClientUpgradeRequest,
    requestUrl: URI,
    responseEffect: Effect[Deferred[Effect, Response]],
    defer: Defer[Effect]
  ): Effect[websocket.api.Session] =
    responseEffect.flatMap { response =>
      effect(webSocketClient.connect(webSocketListener(response), requestUrl, upgradeRequest), defer)
    }

  private def createWebSocketRequest(httpContext: Context, requestUrl: URI): ClientUpgradeRequest = {
    // Headers
    val transportRequest = httpContext.transport.map(_.request).getOrElse(httpClient.newRequest(requestUrl))
    val headers =
      transportRequest.getHeaders.asScala.map(field => field.getName -> field.getValue) ++ httpContext.headers
    val request = new ClientUpgradeRequest
    headers.toSeq.groupBy(_._1).view.mapValues(_.map(_._2)).toSeq.foreach { case (name, values) =>
      request.setHeader(name, values.asJava)
    }

    // Timeout
    val timeout = httpContext.timeout.map(_.toMillis).getOrElse(transportRequest.getTimeout)
    request.setTimeout(timeout, TimeUnit.MILLISECONDS)
    request
  }

  private def webSocketListener(response: Deferred[Effect, Response]): WebSocketListener = new WebSocketListener {

    override def onWebSocketBinary(payload: Array[Byte], offset: Int, length: Int): Unit = {
      val message = util.Arrays.copyOfRange(payload, offset, offset + length)
      val responseBody = message.toInputStream
      system.run(response.succeed((responseBody, None, Seq())).asInstanceOf[Effect[Any]])
    }

    override def onWebSocketError(error: Throwable): Unit = {
      system.run(response.fail(error))
    }
  }

  private def responseContext(response: Response): Context = {
    val (_, statusCode, headers) = response
    statusCode.map(defaultContext.statusCode).getOrElse(defaultContext).headers(headers*)
  }

  private def httpResponse(response: api.Response, responseBody: Array[Byte]): Response = {
    val headers = response.getHeaders.asScala.map(field => field.getName -> field.getValue).toSeq
    (responseBody.toInputStream, Some(response.getStatus), headers)
  }

  private def effect[T](completableFuture: => CompletableFuture[T], defer: Defer[Effect]): Effect[T] =
    defer.deferred[T].flatMap { deferred =>
      Try(completableFuture).pureFold(
        exception => deferred.fail(exception).run,
        value => {
          value.handle { case (result, error) =>
            Option(result).map { value =>
              deferred.succeed(value).run
            }.getOrElse {
              deferred.fail(Option(error).getOrElse {
                new IllegalStateException("Missing completable future result")
              }).run
            }
          }
          ()
        }
      )
      deferred.effect
    }

  private def withDefer[T](function: Defer[Effect] => Effect[T]): Effect[T] = system match {
    case defer: Defer[?] => function(defer.asInstanceOf[Defer[Effect]])
    case _ => system.failed(new IllegalArgumentException(
        s"${Protocol.WebSocket} not supported for effect system without deferred effect support: ${system.getClass.getName}"
      ))
  }
}

object JettyClient {

  /** Request context type. */
  type Context = HttpContext[Session]

  /** Default Jetty HTTP client. */
  def defaultClient: HttpClient = new HttpClient()

  final case class Session(request: Request)

  object Session {
    /** Implicit default context value. */
    implicit val defaultContext: HttpContext[Session] = HttpContext()
  }
}
