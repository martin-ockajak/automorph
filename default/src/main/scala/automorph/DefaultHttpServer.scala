package automorph

import io.undertow.Undertow
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.Http
import automorph.transport.http.server.UndertowServer.defaultBuilder
import automorph.transport.http.server.UndertowServer
import automorph.spi.{EffectSystem, MessageCodec}
import automorph.transport.http.endpoint.UndertowHttpEndpoint.Context
import scala.concurrent.{ExecutionContext, Future}

object DefaultHttpServer {

  /** Default server type. */
  type Type[Node, Codec <: MessageCodec[Node], Effect[_]] = UndertowServer[Node, Codec, Effect]

  /** Request context type. */
  type Context = UndertowServer.Context

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
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @return RPC server
   */
  def apply[Node, Codec <: MessageCodec[Node], Effect[_]](
    handler: Handler[Node, Codec, Effect, Context],
    runEffect: Effect[Any] => Unit,
    port: Int,
    path: String = "/",
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  ): Type[Node, Codec, Effect] = UndertowServer(handler, runEffect, port, path, exceptionToStatusCode, webSocket, builder)

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
  def system[Effect[_]](
    system: EffectSystem[Effect],
    runEffect: Effect[Any] => Unit,
    bindApis: DefaultHandler.Type[Effect, Context] => DefaultHandler.Type[Effect, Context],
    port: Int,
    path: String = "/",
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  ): Type[DefaultMessageCodec.Node, DefaultMessageCodec.Type, Effect] = {
    val handler = bindApis(Handler.protocol(DefaultRpcProtocol()).system(system).context[DefaultHttpServer.Context])
    DefaultHttpServer(handler, runEffect, port, path, exceptionToStatusCode, webSocket, builder)
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
  def async(
    bindApis: DefaultHandler.Type[Future, Context] => DefaultHandler.Type[Future, Context],
    port: Int,
    path: String = "/",
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  )(implicit executionContext: ExecutionContext): Type[DefaultMessageCodec.Node, DefaultMessageCodec.Type, Future] = {
    Seq(executionContext)
    val handler = bindApis(DefaultHandler.async)
    val runEffect = (_: Future[Any]) => ()
    DefaultHttpServer(handler, runEffect, port, path, exceptionToStatusCode, webSocket, builder)
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
  def sync(
    bindApis: DefaultHandler.Type[Identity, Context] => DefaultHandler.Type[Identity, Context],
    port: Int,
    path: String = "/",
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  ): Type[DefaultMessageCodec.Node, DefaultMessageCodec.Type, Identity] = {
    val handler = bindApis(DefaultHandler.sync)
    val runEffect = (_: Identity[Any]) => ()
    DefaultHttpServer(handler, runEffect, port, path, exceptionToStatusCode, webSocket, builder)
  }
}
