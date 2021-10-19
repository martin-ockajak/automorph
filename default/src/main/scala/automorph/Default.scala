package automorph

import automorph.codec.json.CirceJsonCodec
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.{FutureSystem, IdentitySystem}
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.Http
import automorph.transport.http.client.SttpClient
import automorph.transport.http.server.UndertowServer
import automorph.transport.http.server.UndertowServer.defaultBuilder
import io.undertow.Undertow
import java.net.URI
import scala.concurrent.{ExecutionContext, Future}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.{HttpURLConnectionBackend, SttpBackend}

/**
 * Default component constructors.
 */
object Default extends DefaultMeta {

  /** Default message node type. */
  type Node = CirceJsonCodec.Node

  /** Default message codec plugin type. */
  type Codec = CirceJsonCodec

  /** Default asynchronous effect type. */
  type AsyncEffect[T] = Future[T]

  /** Default synchronous effect type. */
  type SyncEffect[T] = Identity[T]

  /** Default asynchronous effect system plugin type. */
  type AsyncSystem = EffectSystem[Future]

  /** Default synchronous effect system plugin type. */
  type SyncSystem = EffectSystem[Identity]

  /**
   * Default RPC client type.
   *
   * @tparam Effect effect type
   * @tparam Context request context type
   */
  type Client[Effect[_], Context] = automorph.Client[Node, Codec, Effect, Context]

  /**
   * Default request handler type.
   *
   * @tparam Effect effect type
   * @tparam Context request context type
   */
  type Handler[Effect[_], Context] = automorph.Handler[Node, Codec, Effect, Context]

  /** Request context type. */
  type HttpClientContext = SttpClient.Context

  /**
   * Default client message transport type.
   *
   * @tparam Effect effect type
   */
  type HttpClientTransport[Effect[_]] = ClientMessageTransport[Effect, HttpClientContext]

  /**
   * Default client type.
   *
   * @tparam Effect effect type
   */
  type HttpClient[Effect[_]] = Client[Effect, HttpClientContext]

  /** Default server type. */
  type HttpServer[Effect[_]] = UndertowServer[Effect]

  /** Request context type. */
  type HttpServerContext = UndertowServer.Context

  /**
   * Default server RPC request handler type.
   *
   * @tparam Effect effect type
   */
  type HttpServerHandler[Effect[_]] = Handler[Effect, HttpServerContext]

  /**
   * Creates a Circe JSON message codec plugin.
   *
   * @see [[https://www.json.org Message format]]
   * @see [[https://circe.github.io/circe Library documentation]]
   * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
   * @return message codec plugin
   */
  def codec: Codec = CirceJsonCodec()

  /**
   * Creates an asynchronous `Future` effect system plugin.
   *
   * @see [[https://docs.scala-lang.org/overviews/core/futures.html Library documentation]]
   * @see [[https://www.scala-lang.org/api/current/scala/concurrent/Future.html Effect type]]
   * @return asynchronous effect system plugin
   */
  def asyncSystem(implicit executionContext: ExecutionContext): AsyncSystem = FutureSystem()

  /**
   * Creates a synchronous identity effect system plugin.
   *
   * @see [[https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem$$Identity.html Effect type]]
   * @return synchronous effect system plugin
   */
  def syncSystem: SyncSystem = IdentitySystem()

  /**
   * Creates a default RPC client with specified message transport plugin.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param transport message transport protocol plugin
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC client
   */
  def client[Effect[_], Context](transport: ClientMessageTransport[Effect, Context]): Client[Effect, Context] =
    Client(protocol, transport)

  /**
   * Creates a default asynchronous RPC request handler with specified effect system plugin and providing given request context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param system effect system plugin
   * @tparam Context request context type
   * @return RPC request handler
   */
  def handler[Effect[_], Context](system: EffectSystem[Effect]): Handler[Effect, Context] =
    Handler(protocol, system)

  /**
   * Creates a default asynchronous RPC request handler using 'Future' as an effect type and providing given request context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param executionContext execution context
   * @tparam Context request context type
   * @return asynchronous RPC request handler
   */
  def asyncHandler[Context](implicit executionContext: ExecutionContext): Handler[Future, Context] =
    Handler(protocol, asyncSystem)

  /**
   * Creates a default synchronous RPC request handler using identity as an effect type and providing given request context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @tparam Context request context type
   * @return synchronous RPC request handler
   */
  def syncHandler[Context]: Handler[Identity, Context] =
    Handler(protocol, syncSystem)

  /**
   * Creates an STTP HTTP & WebSocket client message transport plugin with specified effect system plugin.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param backend client message transport backend
   * @param system effect system plugin
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @tparam Effect effect type
   * @return client message transport plugin
   */
  def httpClientTransport[Effect[_]](
    url: URI,
    method: String,
    backend: SttpBackend[Effect, _],
    system: EffectSystem[Effect],
    webSocket: Boolean = false
  ): HttpClientTransport[Effect] = SttpClient(url, method, backend, system, webSocket)

  /**
   * Creates an asynchronous STTP HTTP & WebSocket client message transport plugin using 'Future' as an effect type.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @param executionContext execution context
   * @return asynchronous client message transport plugin
   */
  def asyncHttpClientTransport(url: URI, method: String, webSocket: Boolean = false)(implicit
    executionContext: ExecutionContext
  ): HttpClientTransport[Future] =
    httpClientTransport(url, method, AsyncHttpClientFutureBackend(), asyncSystem, webSocket)

  /**
   * Creates a synchronous STTP HTTP & WebSocket client message transport plugin using identity as an effect type.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @return synchronous client message transport plugin
   */
  def syncHttpClientTransport(url: URI, method: String, webSocket: Boolean = false): HttpClientTransport[Identity] = {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    httpClientTransport(url, method, HttpURLConnectionBackend(), syncSystem, webSocket)
  }

  /**
   * Creates a JSON-RPC over STTP HTTP & WebSocket client with specified effect system plugin.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param backend STTP client backend
   * @param system effect system plugin
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @tparam Effect effect type
   * @return RPC client
   */
  def httpClient[Effect[_]](
    url: URI,
    method: String,
    backend: SttpBackend[Effect, _],
    system: EffectSystem[Effect],
    webSocket: Boolean = false
  ): HttpClient[Effect] = client(httpClientTransport(url, method, backend, system, webSocket))

  /**
   * Creates an asynchronous JSON-RPC over STTP HTTP & WebSocket client using 'Future' as an effect type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @param executionContext execution context
   * @return asynchronous RPC client
   */
  def asyncHttpClient(url: URI, method: String, webSocket: Boolean = false)(implicit
    executionContext: ExecutionContext
  ): HttpClient[Future] =
    httpClient(url, method, AsyncHttpClientFutureBackend(), asyncSystem, webSocket)

  /**
   * Creates a synchronous JSON-RPC over STTP HTTP & WebSocket client using identity as an effect type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @return synchronous RPC client
   */
  def syncHttpClient(url: URI, method: String, webSocket: Boolean = false): HttpClient[Identity] = {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    httpClient(url, method, HttpURLConnectionBackend(), syncSystem, webSocket)
  }

  /**
   * Creates an Undertow RPC over HTTP & WebSocket server with specified RPC request handler.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see [[https://undertow.io Library documentation]]
   * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param handler RPC request handler
   * @param runEffect executes specified effect asynchronously
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param builder Undertow web server builder
   * @tparam Effect effect type
   * @return RPC server
   */
  def httpServer[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, HttpServerContext],
    runEffect: Effect[Any] => Unit,
    port: Int,
    path: String = "/",
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  ): HttpServer[Effect] = UndertowServer(handler, runEffect, port, path, exceptionToStatusCode, webSocket, builder)

  /**
   * Creates an Undertow JSON-RPC over HTTP & WebSocket server with specified effect system plugin.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see [[https://undertow.io Library documentation]]
   * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param system effect system plugin
   * @param runEffect executes specified effect asynchronously
   * @param bindApis function to bind APIs to the underlying handler
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param builder Undertow web server builder
   * @tparam Effect effect type
   * @return RPC server
   */
  def systemHttpServer[Effect[_]](
    system: EffectSystem[Effect],
    runEffect: Effect[Any] => Unit,
    bindApis: HttpServerHandler[Effect] => HttpServerHandler[Effect],
    port: Int,
    path: String = "/",
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  ): HttpServer[Effect] = {
    val handler = bindApis(Handler.protocol(DefaultRpcProtocol()).system(system).context[DefaultHttpServer.Context])
    httpServer(handler, runEffect, port, path, exceptionToStatusCode, webSocket, builder)
  }

  /**
   * Creates an asynchronous Undertow JSON-RPC over HTTP & WebSocket server using 'Future' as an effect type.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see [[https://undertow.io Library documentation]]
   * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param bindApis function to bind APIs to the underlying handler
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param builder Undertow web server builder
   * @param executionContext execution context
   * @return asynchronous RPC server
   */
  def asyncHttpServer(
    bindApis: HttpServerHandler[Future] => HttpServerHandler[Future],
    port: Int,
    path: String = "/",
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  )(implicit executionContext: ExecutionContext): HttpServer[Future] = {
    Seq(executionContext)
    val handler = bindApis(DefaultHandler.async)
    val runEffect = (_: Future[Any]) => ()
    httpServer(handler, runEffect, port, path, exceptionToStatusCode, webSocket, builder)
  }

  /**
   * Creates a synchronous Undertow JSON-RPC over HTTP & WebSocket server using identity as an effect type.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see [[https://undertow.io Library documentation]]
   * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param bindApis function to bind APIs to the underlying handler
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param builder Undertow web server builder
   * @return synchronous RPC server
   */
  def syncHttpServer(
    bindApis: HttpServerHandler[Identity] => HttpServerHandler[Identity],
    port: Int,
    path: String = "/",
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  ): HttpServer[Identity] = {
    val handler = bindApis(DefaultHandler.sync)
    val runEffect = (_: Identity[Any]) => ()
    httpServer(handler, runEffect, port, path, exceptionToStatusCode, webSocket, builder)
  }
}
