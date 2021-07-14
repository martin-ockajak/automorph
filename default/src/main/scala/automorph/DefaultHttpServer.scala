package automorph

import io.undertow.Undertow
import automorph.system.IdentityBackend.Identity
import automorph.transport.http.endpoint.UndertowJsonRpcHandler.defaultErrorStatus
import automorph.transport.http.server.UndertowServer.defaultBuilder
import automorph.transport.http.endpoint.UndertowJsonRpcHandler
import automorph.transport.http.server.UndertowServer
import automorph.spi.EffectSystem
import scala.concurrent.{ExecutionContext, Future}

case object DefaultHttpServer {

  /** Default server type. */
  type Type = UndertowServer

  /** Request context type. */
  type Context = UndertowServer.Context

  /**
   * Default server request handler type.
   *
   * @tparam Effect effec type
   */
  type Handler[Effect[_]] = DefaultHandler.Type[Effect, UndertowServer.Context]

  /**
   * Creates a default server using HTTP as message transport protocol with specified ''backend'' plugin.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://www.jsonrpc.org/specification protocol specification]]
   * @see [[https://undertow.io HTTP Server Documentation]]
   * @param backend effect system plugin
   * @param runEffect effect execution function
   * @param bindApis function to bind APIs to the underlying handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP URL path (default: /)
   * @param builder Undertow web server builder
   * @param errorStatus error code to HTTP status mapping function
   * @tparam Effect effect type
   * @return server
   */
  def apply[Effect[_]](
    backend: EffectSystem[Effect],
    runEffect: Effect[Any] => Any,
    bindApis: Handler[Effect] => Handler[Effect],
    port: Int,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  ): Type = {
    val handler = bindApis(DefaultHandler[Effect, UndertowServer.Context](backend))
    UndertowServer(UndertowJsonRpcHandler(handler, runEffect, errorStatus), port, urlPath, builder)
  }

  /**
   * Creates a default asynchronous server using HTTP as message transport protocol and 'Future' as an effect type.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://www.jsonrpc.org/specification protocol specification]]
   * @param bindApis function to bind APIs to the underlying handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP URL path (default: /)
   * @param builder Undertow web server builder
   * @param errorStatus error code to HTTP status mapping function
   * @param executionContext execution context
   * @return asynchronous server
   */
  def async(
    bindApis: Handler[Future] => Handler[Future],
    port: Int,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  )(implicit executionContext: ExecutionContext): Type = {
    Seq(executionContext)
    val handler = bindApis(DefaultHandler.async())
    val runEffect = (_: Future[Any]) => ()
    UndertowServer(UndertowJsonRpcHandler(handler, runEffect, errorStatus), port, urlPath, builder)
  }

  /**
   * Creates a default synchronous server using HTTP as message transport protocol and identity as an effect type.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://www.jsonrpc.org/specification protocol specification]]
   * @param bindApis function to bind APIs to the underlying handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP URL path (default: /)
   * @param builder Undertow web server builder
   * @param errorStatus error code to HTTP status mapping function
   * @return synchronous server
   */
  def sync(
    bindApis: Handler[Identity] => Handler[Identity],
    port: Int,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  ): Type = {
    val handler = bindApis(DefaultHandler.sync())
    val runEffect = (_: Identity[Any]) => ()
    UndertowServer(UndertowJsonRpcHandler(handler, runEffect, errorStatus), port, urlPath, builder)
  }
}
