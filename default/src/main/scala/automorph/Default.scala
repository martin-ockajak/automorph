package automorph

import automorph.meta.DefaultMeta
import automorph.spi.{AsyncEffectSystem, ClientMessageTransport, EffectSystem}
import automorph.system.IdentitySystem.Identity
import automorph.system.{FutureSystem, IdentitySystem}
import automorph.transport.http.client.HttpClient
import automorph.transport.http.server.UndertowServer
import automorph.transport.http.server.UndertowServer.defaultBuilder
import automorph.transport.http.{HttpContext, HttpMethod}
import io.undertow.Undertow
import java.net.URI
import scala.concurrent.{ExecutionContext, Future}

/** Default component constructors. */
object Default extends DefaultMeta {

  /**
   * Default RPC client type.
   *
   * @tparam Effect
   *   effect type
   * @tparam Context
   *   message context type
   */
  type Client[Effect[_], Context] = automorph.Client[Node, Codec, Effect, Context]

  /**
   * Default RPC request handler type.
   *
   * @tparam Effect
   *   effect type
   * @tparam Context
   *   message context type
   */
  type Handler[Effect[_], Context] = automorph.Handler[Node, Codec, Effect, Context]

  /** Request context type. */
  type ClientContext = HttpClient.Context

  /** Request context type. */
  type ServerContext = UndertowServer.Context

  /** Default server type. */
  type Server[Effect[_]] = UndertowServer[Effect]

  /** Default asynchronous effect type. */
  type AsyncEffect[T] = Future[T]

  /** Default synchronous effect type. */
  type SyncEffect[T] = Identity[T]

  /**
   * Server API binding function.
   *
   * @tparam Effect
   *   effect type
   */
  type ServerApiBinder[Effect[_]] = Handler[Effect, ServerContext] => Handler[Effect, ServerContext]

  /**
   * Server building function.
   *
   * @tparam Effect
   *   effect type
   */
  type ServerBuilder[Effect[_]] = ServerApiBinder[Effect] => Server[Effect]

  /**
   * Creates a standard JRE JSON-RPC over HTTP & WebSocket client with specified effect system plugin.
   *
   * The client can be used to perform type-safe remote API calls or send one-way messages.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see
   *   [[https://openjdk.org/groups/net/httpclient/intro.html documentation]]
   * @see
   *   [[https://docs.oracle.com/en/java/javase/19/docs/api/java.net.http/java/net/http/HttpClient.html API]]
   * @param effectSystem
   *   effect system plugin
   * @param url
   *   HTTP endpoint URL
   * @param method
   *   HTTP request method
   * @tparam Effect
   *   effect type
   * @return
   *   RPC client
   */
  def client[Effect[_]](
    effectSystem: EffectSystem[Effect],
    url: URI,
    method: HttpMethod = HttpMethod.Post,
  ): Client[Effect, ClientContext] =
    client(clientTransport(effectSystem, url, method))

  /**
   * Creates a JSON-RPC client with specified message transport plugin.
   *
   * The client can be used to perform type-safe remote API calls or send one-way messages.
   *
   * @param clientMessageTransport
   *   message transport protocol plugin
   * @tparam Effect
   *   effect type
   * @tparam Context
   *   message context type
   * @return
   *   RPC client
   */
  def client[Effect[_], Context](
    clientMessageTransport: ClientMessageTransport[Effect, Context]
  ): Client[Effect, Context] =
    Client(rpcProtocol, clientMessageTransport)

  /**
   * Creates a standard JRE HTTP & WebSocket client message transport plugin with specified effect system plugin.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see
   *   [[https://openjdk.org/groups/net/httpclient/intro.html documentation]]
   * @see
   *   [[https://docs.oracle.com/en/java/javase/19/docs/api/java.net.http/java/net/http/HttpClient.html API]]
   * @param system
   *   effect system plugin
   * @param url
   *   HTTP endpoint URL
   * @param method
   *   HTTP request method
   * @tparam Effect
   *   effect type
   * @return
   *   client message transport plugin
   */
  def clientTransport[Effect[_]](
    system: EffectSystem[Effect],
    url: URI,
    method: HttpMethod = HttpMethod.Post,
  ): ClientMessageTransport[Effect, ClientContext] =
    HttpClient(system, url, method)

  /**
   * Creates a standard JRE JSON-RPC over HTTP & WebSocket client with default RPC protocol using 'Future' as an effect
   * type.
   *
   * The client can be used to perform type-safe remote API calls or send one-way messages.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see
   *   [[https://openjdk.org/groups/net/httpclient/intro.html documentation]]
   * @see
   *   [[https://docs.oracle.com/en/java/javase/19/docs/api/java.net.http/java/net/http/HttpClient.html API]]
   * @param url
   *   HTTP endpoint URL
   * @param method
   *   HTTP request method
   * @param executionContext
   *   execution context
   * @return
   *   asynchronous RPC client
   */
  def clientAsync(url: URI, method: HttpMethod = HttpMethod.Post)(implicit
    executionContext: ExecutionContext
  ): Client[AsyncEffect, ClientContext] =
    client(clientTransportAsync(url, method))

  /**
   * Creates a standard JRE JSON-RPC over HTTP & WebSocket client with default RPC protocol using identity as an effect
   * type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see
   *   [[https://openjdk.org/groups/net/httpclient/intro.html documentation]]
   * @see
   *   [[https://docs.oracle.com/en/java/javase/19/docs/api/java.net.http/java/net/http/HttpClient.html API]]
   * @param url
   *   HTTP endpoint URL
   * @param method
   *   HTTP request method
   * @return
   *   synchronous RPC client
   */
  def clientSync(url: URI, method: HttpMethod = HttpMethod.Post): Client[SyncEffect, ClientContext] =
    client(clientTransportSync(url, method))

  /**
   * Creates a JSON-RPC request handler with specified effect system plugin while providing given message context type.
   *
   * The handler can be used by a server to invoke bound API methods based on supplied RPC requests.
   *
   * @param system
   *   effect system plugin
   * @tparam Context
   *   message context type
   * @return
   *   RPC request handler
   */
  def handler[Effect[_], Context](system: EffectSystem[Effect]): Handler[Effect, Context] =
    Handler(rpcProtocol, system)

  /**
   * Creates a JSON-RPC request handler using identity as an effect type while providing given message context type.
   *
   * The handler can be used by a server to invoke bound API methods based on supplied RPC requests.
   *
   * @tparam Context
   *   message context type
   * @return
   *   synchronous RPC request handler
   */
  def handlerSync[Context]: Handler[SyncEffect, Context] =
    Handler(rpcProtocol, effectSystemSync)

  /**
   * Creates a JSON-RPC request handler using 'Future' as an effect type while providing given message context type.
   *
   * The handler can be used by a server to invoke bound API methods based on supplied RPC requests.
   *
   * @param executionContext
   *   execution context
   * @tparam Context
   *   message context type
   * @return
   *   asynchronous RPC request handler
   */
  def handlerAsync[Context](implicit executionContext: ExecutionContext): Handler[AsyncEffect, Context] =
    Handler(rpcProtocol, effectSystemAsync)

  /**
   * Creates an Undertow RPC over HTTP & WebSocket server with specified RPC request handler.
   *
   * The server can be used to serve remote API requests using specific message transport protocol and invoke bound
   * API methods to process them.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see
   *   [[https://undertow.io Library documentation]]
   * @see
   *   [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param handler
   *   RPC request handler
   * @param port
   *   port to listen on for HTTP connections
   * @param path
   *   HTTP URL path (default: /)
   * @param methods
   *   allowed HTTP request methods (default: any)
   * @param webSocket
   *   both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param mapException
   *   maps an exception to a corresponding HTTP status code
   * @param builder
   *   Undertow web server builder
   * @tparam Effect
   *   effect type
   * @return
   *   RPC server
   */
  def server[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, ServerContext],
    port: Int,
    path: String = "/",
    methods: Iterable[HttpMethod] = HttpMethod.values,
    webSocket: Boolean = true,
    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
    builder: Undertow.Builder = defaultBuilder,
  ): Server[Effect] =
    UndertowServer(handler, port, path, methods, webSocket, mapException, builder)

  /**
   * Creates an Undertow JSON-RPC over HTTP & WebSocket server with specified effect system plugin.
   *
   * Resulting function requires:
   *   - API binding function - binds APIs to the underlying handler
   *
   * The server can be used to serve remote API requests using specific message transport protocol and invoke bound
   * API methods to process them.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see
   *   [[https://undertow.io Library documentation]]
   * @see
   *   [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param effectSystem
   *   effect system plugin
   * @param port
   *   port to listen on for HTTP connections
   * @param path
   *   HTTP URL path (default: /)
   * @param methods
   *   allowed HTTP request methods (default: any)
   * @param webSocket
   *   both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param mapException
   *   maps an exception to a corresponding HTTP status code
   * @param builder
   *   Undertow web server builder
   * @tparam Effect
   *   effect type
   * @return
   *   creates RPC server using supplied API binding function and asynchronous effect execution function
   */
  def serverBuilder[Effect[_]](
    effectSystem: EffectSystem[Effect],
    port: Int,
    path: String = "/",
    methods: Iterable[HttpMethod] = HttpMethod.values,
    webSocket: Boolean = true,
    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
    builder: Undertow.Builder = defaultBuilder,
  ): ServerBuilder[Effect] =
    (serverApiBinder: ServerApiBinder[Effect]) => {
      val handler = serverApiBinder(Handler.protocol(rpcProtocol[ServerContext]).system(effectSystem))
      server(handler, port, path, methods, webSocket, mapException, builder)
    }

  /**
   * Creates an Undertow JSON-RPC server builder over HTTP & WebSocket using identity as an effect type.
   *
   * Resulting function requires:
   *   - API binding function - binds APIs to the underlying handler
   *
   * The server can be used to serve remote API requests using specific message transport protocol while invoking
   * server to process them.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see
   *   [[https://undertow.io Library documentation]]
   * @see
   *   [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param port
   *   port to listen on for HTTP connections
   * @param path
   *   HTTP URL path (default: /)
   * @param methods
   *   allowed HTTP request methods (default: any)
   * @param webSocket
   *   both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param mapException
   *   maps an exception to a corresponding HTTP status code
   * @param builder
   *   Undertow web server builder
   * @return
   *   synchronous RPC server builder using supplied API binding function
   */
  def serverBuilderSync(
    port: Int,
    path: String = "/",
    methods: Iterable[HttpMethod] = HttpMethod.values,
    webSocket: Boolean = true,
    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
    builder: Undertow.Builder = defaultBuilder,
  ): ServerBuilder[SyncEffect] =
    (serverApiBinder: ServerApiBinder[SyncEffect]) => {
      val handler = serverApiBinder(handlerSync)
      server(handler, port, path, methods, webSocket, mapException, builder)
    }

  /**
   * Creates an Undertow JSON-RPC server builder over HTTP & WebSocket using 'Future' as an effect type.
   *
   * Resulting function requires:
   *   - API binding function - binds APIs to the underlying handler
   *
   * The server can be used to serve remote API requests using specific message transport protocol and invoke bound
   * API method to process them.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see
   *   [[https://undertow.io Library documentation]]
   * @see
   *   [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param port
   *   port to listen on for HTTP connections
   * @param path
   *   HTTP URL path (default: /)
   * @param methods
   *   allowed HTTP request methods (default: any)
   * @param webSocket
   *   both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param mapException
   *   maps an exception to a corresponding HTTP status code
   * @param builder
   *   Undertow web server builder
   * @param executionContext
   *   execution context
   * @return
   *   asynchronous RPC server builder using supplied API binding function
   */
  def serverBuilderAsync(
    port: Int,
    path: String = "/",
    methods: Iterable[HttpMethod] = HttpMethod.values,
    webSocket: Boolean = true,
    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
    builder: Undertow.Builder = defaultBuilder,
  )(implicit executionContext: ExecutionContext): ServerBuilder[AsyncEffect] =
    (serverApiBinder: ServerApiBinder[AsyncEffect]) => {
      val handler = serverApiBinder(handlerAsync)
      server(handler, port, path, methods, webSocket, mapException, builder)
    }

  /**
   * Creates a synchronous identity effect system plugin.
   *
   * @see
   *   [[https://www.scala-lang.org/files/archive/api/3.x/ documentation]]
   * @see
   *   [[https://scala-lang.org/api/3.x/scala/Predef$.html#identity-957 Effect type]]
   * @return
   *   synchronous effect system plugin
   */
  def effectSystemSync: EffectSystem[SyncEffect] =
    IdentitySystem()

  /**
   * Creates an asynchronous `Future` effect system plugin.
   *
   * @see
   *   [[https://docs.scala-lang.org/overviews/core/futures.html Library documentation]]
   * @see
   *   [[https://scala-lang.org/api/3.x/scala/concurrent/Future.html Effect type]]
   * @return
   *   asynchronous effect system plugin
   */
  def effectSystemAsync(implicit executionContext: ExecutionContext): AsyncEffectSystem[AsyncEffect] =
    FutureSystem()

  /**
   * Creates a standard JRE HTTP & WebSocket client message transport plugin using identity as an effect type.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see
   *   [[https://openjdk.org/groups/net/httpclient/intro.html documentation]]
   * @see
   *   [[https://docs.oracle.com/en/java/javase/19/docs/api/java.net.http/java/net/http/HttpClient.html API]]
   * @param url
   *   HTTP endpoint URL
   * @param method
   *   HTTP request method
   * @return
   *   synchronous client message transport plugin
   */
  def clientTransportSync(
    url: URI,
    method: HttpMethod = HttpMethod.Post,
  ): ClientMessageTransport[SyncEffect, ClientContext] =
    clientTransport(effectSystemSync, url, method)

  /**
   * Creates a standard JRE HTTP & WebSocket client message transport plugin using 'Future' as an effect type.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
   * @see
   *   [[https://openjdk.org/groups/net/httpclient/intro.html documentation]]
   * @see
   *   [[https://docs.oracle.com/en/java/javase/19/docs/api/java.net.http/java/net/http/HttpClient.html API]]
   * @param url
   *   HTTP endpoint URL
   * @param method
   *   HTTP request method
   * @param executionContext
   *   execution context
   * @return
   *   asynchronous client message transport plugin
   */
  def clientTransportAsync(url: URI, method: HttpMethod = HttpMethod.Post)(implicit
    executionContext: ExecutionContext
  ): ClientMessageTransport[AsyncEffect, ClientContext] =
    clientTransport(effectSystemAsync, url, method)
}
