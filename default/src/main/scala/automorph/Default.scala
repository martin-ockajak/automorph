package automorph

import automorph.meta.DefaultMeta
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.IdentitySystem.Identity
import automorph.system.{FutureSystem, IdentitySystem}
import automorph.transport.http.HttpContext
import automorph.transport.http.client.HttpClient
import automorph.transport.http.server.UndertowServer
import automorph.transport.http.server.UndertowServer.defaultBuilder
import io.undertow.Undertow
import java.net.URI
import scala.concurrent.{ExecutionContext, Future}

/** Default component constructors. */
object Default extends DefaultMeta {

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
   * @tparam Context message context type
   */
  type Client[Effect[_], Context] = automorph.Client[Node, Codec, Effect, Context]

  /**
   * Default RPC request handler type.
   *
   * @tparam Effect effect type
   * @tparam Context message context type
   */
  type Handler[Effect[_], Context] = automorph.Handler[Node, Codec, Effect, Context]

  /** Request context type. */
  type ClientContext = HttpClient.Context

  /**
   * Default RPC client message transport type.
   *
   * @tparam Effect effect type
   */
  type ClientTransport[Effect[_]] = ClientMessageTransport[Effect, ClientContext]

  /** Default server type. */
  type Server[Effect[_]] = UndertowServer[Effect]

  /** Request context type. */
  type ServerContext = UndertowServer.Context

  /**
   * Default RPC server request handler type.
   *
   * @tparam Effect effect type
   */
  type ServerHandler[Effect[_]] = Handler[Effect, ServerContext]

  /**
   * Asynchronous effect execution function type.
   *
   * @tparam Effect effect type
   */
  type Run[Effect[_]] = Effect[Any] => Unit

  /**
   * Server API binding function.
   *
   * @tparam Effect effect type
   */
  type ServerBindApis[Effect[_]] = ServerHandler[Effect] => ServerHandler[Effect]

  /**
   * Creates an asynchronous `Future` effect system plugin.
   *
   * @see [[https://docs.scala-lang.org/overviews/core/futures.html Library documentation]]
   * @see [[https://www.scala-lang.org/api/current/scala/concurrent/Future.html Effect type]]
   * @return asynchronous effect system plugin
   */
  def systemAsync(implicit executionContext: ExecutionContext): AsyncSystem =
    FutureSystem()

  /**
   * Creates a synchronous identity effect system plugin.
   *
   * @see [[https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem$$Identity.html Effect type]]
   * @return synchronous effect system plugin
   */
  def systemSync: SyncSystem =
    IdentitySystem()

  /**
   * Creates a JSON-RPC client with specified message transport plugin.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param transport message transport protocol plugin
   * @tparam Effect effect type
   * @tparam Context message context type
   * @return RPC client
   */
  def client[Effect[_], Context](transport: ClientMessageTransport[Effect, Context]): Client[Effect, Context] =
    Client(protocol, transport)

  /**
   * Creates a JSON-RPC request handler with specified effect system plugin while providing given message context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param system effect system plugin
   * @tparam Context message context type
   * @return RPC request handler
   */
  def handler[Effect[_], Context](system: EffectSystem[Effect]): Handler[Effect, Context] =
    Handler(protocol, system)

  /**
   * Creates a JSON-RPC request handler using 'Future' as an effect type while providing given message context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param executionContext execution context
   * @tparam Context message context type
   * @return asynchronous RPC request handler
   */
  def handlerAsync[Context](implicit executionContext: ExecutionContext): Handler[Future, Context] =
    Handler(protocol, systemAsync)

  /**
   * Creates a JSON-RPC request handler using identity as an effect type while providing given message context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @tparam Context message context type
   * @return synchronous RPC request handler
   */
  def handlerSync[Context]: Handler[Identity, Context] =
    Handler(protocol, systemSync)

  /**
   * Creates a standard JRE HTTP & WebSocket client message transport plugin with specified effect system plugin.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param system effect system plugin
   * @tparam Effect effect type
   * @return creates client message transport plugin using supplied asynchronous effect execution function
   */
  def clientTransport[Effect[_]](
    url: URI,
    method: String,
    system: EffectSystem[Effect]
  ): Run[Effect] => ClientTransport[Effect] =
    HttpClient.create(url, method, system)

  /**
   * Creates a standard JRE HTTP & WebSocket client message transport plugin using 'Future' as an effect type.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param executionContext execution context
   * @return asynchronous client message transport plugin
   */
  def clientTransportAsync(url: URI, method: String)(implicit
    executionContext: ExecutionContext
  ): ClientTransport[Future] =
    clientTransport(url, method, systemAsync) { effect =>
      effect
      ()
    }

  /**
   * Creates a standard JRE HTTP & WebSocket client message transport plugin using identity as an effect type.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @return synchronous client message transport plugin
   */
  def clientTransportSync(url: URI, method: String): ClientTransport[Identity] =
    clientTransport(url, method, systemSync) { effect =>
      effect
      ()
    }

  /**
   * Creates a standard JRE JSON-RPC over HTTP & WebSocket client with specified effect system plugin.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param system effect system plugin
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @tparam Effect effect type
   * @return creates RPC client using supplied asynchronous effect execution function
   */
  def client[Effect[_]](
    url: URI,
    method: String,
    system: EffectSystem[Effect]
  ): Run[Effect] => Client[Effect, ClientContext] =
    runEffect => client(clientTransport(url, method, system)(runEffect))

  /**
   * Creates a standard JRE JSON-RPC over HTTP & WebSocket client with default RPC protocol using 'Future' as an effect type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param executionContext execution context
   * @return asynchronous RPC client
   */
  def clientAsync(url: URI, method: String)(implicit
    executionContext: ExecutionContext
  ): Client[Future, ClientContext] =
    client(clientTransportAsync(url, method))

  /**
   * Creates a standard JRE JSON-RPC over HTTP & WebSocket client with default RPC protocol using identity as an effect type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @return synchronous RPC client
   */
  def clientSync(url: URI, method: String): Client[Identity, ClientContext] =
    client(clientTransportSync(url, method))

  /**
   * Creates an Undertow RPC over HTTP & WebSocket server with specified RPC request handler.
   *
   * Resulting function requires:
   * - effect execution function - executes specified effect asynchronously
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see [[https://undertow.io Library documentation]]
   * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param handler RPC request handler
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param methods allowed HTTP request methods (default: POST, GET, PUT, DELETE)
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param mapException maps an exception to a corresponding HTTP status code
   * @param builder Undertow web server builder
   * @tparam Effect effect type
   * @return creates RPC server using supplied asynchronous effect execution function
   */
  def server[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, ServerContext],
    port: Int,
    path: String = "/",
    methods: Iterable[String] = Seq("POST", "GET", "PUT", "DELETE"),
    webSocket: Boolean = true,
    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
    builder: Undertow.Builder = defaultBuilder
  ): Run[Effect] => Server[Effect] =
    (runEffect: Run[Effect]) =>
      UndertowServer.create(handler, port, path, methods, webSocket, mapException, builder)(runEffect)

  /**
   * Creates an Undertow JSON-RPC over HTTP & WebSocket server with specified effect system plugin.
   *
   * Resulting function requires:
   * - API binding function - binds APIs to the underlying handler
   * - effect execution function - executes specified effect asynchronously
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see [[https://undertow.io Library documentation]]
   * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param system effect system plugin
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param methods allowed HTTP request methods (default: POST, GET, PUT, DELETE)
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param mapException maps an exception to a corresponding HTTP status code
   * @param builder Undertow web server builder
   * @tparam Effect effect type
   * @return creates RPC server using supplied API binding function and asynchronous effect execution function
   */
  def serverSystem[Effect[_]](
    system: EffectSystem[Effect],
    port: Int,
    path: String = "/",
    methods: Iterable[String] = Seq("POST", "GET", "PUT", "DELETE"),
    webSocket: Boolean = true,
    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
    builder: Undertow.Builder = defaultBuilder
  ): ServerBindApis[Effect] => Run[Effect] => Server[Effect] =
    (bindApis: ServerBindApis[Effect]) => {
      val handler = bindApis(Handler.protocol(protocol).system(system).context[ServerContext])
      server(handler, port, path, methods, webSocket, mapException, builder)
    }

  /**
   * Creates an Undertow JSON-RPC over HTTP & WebSocket server using 'Future' as an effect type.
   *
   * Resulting function requires:
   * - API binding function - binds APIs to the underlying handler
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see [[https://undertow.io Library documentation]]
   * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param methods allowed HTTP request methods (default: POST, GET, PUT, DELETE)
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param mapException maps an exception to a corresponding HTTP status code
   * @param builder Undertow web server builder
   * @param executionContext execution context
   * @return asynchronous RPC server using supplied API binding function
   */
  def serverAsync(
    port: Int,
    path: String = "/",
    methods: Iterable[String] = Seq("POST", "GET", "PUT", "DELETE"),
    webSocket: Boolean = true,
    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
    builder: Undertow.Builder = defaultBuilder
  )(implicit executionContext: ExecutionContext): ServerBindApis[Future] => Server[Future] =
    (bindApis: ServerBindApis[Future]) => {
      val handler = bindApis(handlerAsync)
      val runEffect = (_: Future[Any]) => ()
      server(handler, port, path, methods, webSocket, mapException, builder)(runEffect)
    }

  /**
   * Creates a Undertow JSON-RPC over HTTP & WebSocket server using identity as an effect type.
   *
   * Resulting function requires:
   * - API binding function - binds APIs to the underlying handler
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see [[https://undertow.io Library documentation]]
   * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param methods allowed HTTP request methods (default: POST, GET, PUT, DELETE)
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param mapException maps an exception to a corresponding HTTP status code
   * @param builder Undertow web server builder
   * @return synchronous RPC server using supplied API binding function
   */
  def serverSync(
    port: Int,
    path: String = "/",
    methods: Iterable[String] = Seq("POST", "GET", "PUT", "DELETE"),
    webSocket: Boolean = true,
    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
    builder: Undertow.Builder = defaultBuilder
  ): ServerBindApis[Identity] => Server[Identity] =
    (bindApis: ServerBindApis[Identity]) => {
      val handler = bindApis(handlerSync)
      val runEffect = (_: Identity[Any]) => ()
      server(handler, port, path, methods, webSocket, mapException, builder)(runEffect)
    }
}
