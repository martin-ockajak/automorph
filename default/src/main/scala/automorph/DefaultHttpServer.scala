package automorph

import io.undertow.Undertow
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.endpoint.UndertowHttpEndpoint.defaultErrorStatus
import automorph.transport.http.server.UndertowServer.{defaultBuilder, Context}
import automorph.transport.http.endpoint.{UndertowHttpEndpoint, UndertowWebSocketEndpoint}
import automorph.transport.http.server.UndertowServer
import automorph.spi.EffectSystem
import scala.concurrent.{ExecutionContext, Future}

case object DefaultHttpServer {

  /** Default server type. */
  type Type[Effect[_]] = UndertowServer[Effect]

  /** Request context type. */
  type Context = UndertowServer.Context

  /**
   * Default server RPC request handler type.
   *
   * @tparam Effect effect type
   */
  type Handler[Effect[_]] = DefaultHandler.Type[Effect, Context]

  /**
   * Creates a default RPC server using HTTP as message transport protocol with specified RPC request ''handler''.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://undertow.io/ HTTP Server Documentation]]
   * @param handler RPC request handler
   * @param runEffect effect execution function
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param errorStatus error code to HTTP status mapping function
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param builder Undertow web server builder
   * @tparam Effect effect type
   * @return RPC server
   */
  def apply[Effect[_]](
    handler: Handler.AnyFormat[Effect, Context],
    runEffect: Effect[Any] => Any,
    port: Int,
    path: String = "/",
    errorStatus: Int => Int = defaultErrorStatus,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  ): Type[Effect] = UndertowServer(handler, runEffect, port, path, errorStatus, webSocket, builder)

  /**
   * Creates a default RPC server using HTTP as message transport protocol with specified effect ''system'' plugin.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://undertow.io/ HTTP Server Documentation]]
   * @param system effect system plugin
   * @param runEffect effect execution function
   * @param bindApis function to bind APIs to the underlying handler
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param errorStatus error code to HTTP status mapping function
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param builder Undertow web server builder
   * @tparam Effect effect type
   * @return RPC server
   */
  def system[Effect[_]](
    system: EffectSystem[Effect],
    runEffect: Effect[Any] => Any,
    bindApis: Handler[Effect] => Handler[Effect],
    port: Int,
    path: String = "/",
    errorStatus: Int => Int = defaultErrorStatus,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  ): Type[Effect] = {
    val handler = bindApis(DefaultHandler(system))
    DefaultHttpServer(handler, runEffect, port, path, errorStatus, webSocket, builder)
  }

  /**
   * Creates a default asynchronous RPC server using HTTP as message transport protocol and 'Future' as an effect type.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @param bindApis function to bind APIs to the underlying handler
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param errorStatus error code to HTTP status mapping function
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param builder Undertow web server builder
   * @param executionContext execution context
   * @return asynchronous RPC server
   */
  def async(
    bindApis: Handler[Future] => Handler[Future],
    port: Int,
    path: String = "/",
    errorStatus: Int => Int = defaultErrorStatus,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  )(implicit executionContext: ExecutionContext): Type[Future] = {
    Seq(executionContext)
    val handler = bindApis(DefaultHandler.async())
    val runEffect = (_: Future[Any]) => ()
    DefaultHttpServer(handler, runEffect, port, path, errorStatus, webSocket, builder)
  }

  /**
   * Creates a default synchronous RPC server using HTTP as message transport protocol and identity as an effect type.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @param bindApis function to bind APIs to the underlying handler
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param errorStatus error code to HTTP status mapping function
   * @param webSocket both HTTP and WebSocket protocols enabled if true, HTTP only if false
   * @param builder Undertow web server builder
   * @return synchronous RPC server
   */
  def sync(
    bindApis: Handler[Identity] => Handler[Identity],
    port: Int,
    path: String = "/",
    errorStatus: Int => Int = defaultErrorStatus,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  ): Type[Identity] = {
    val handler = bindApis(DefaultHandler.sync())
    val runEffect = (_: Identity[Any]) => ()
    DefaultHttpServer(handler, runEffect, port, path, errorStatus, webSocket, builder)
  }
}
