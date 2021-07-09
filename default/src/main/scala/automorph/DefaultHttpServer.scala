package automorph

import io.undertow.Undertow
import automorph.backend.IdentityBackend.Identity
import automorph.server.http.UndertowJsonRpcHandler.defaultErrorStatus
import automorph.server.http.UndertowServer.defaultBuilder
import automorph.server.http.{UndertowJsonRpcHandler, UndertowServer}
import automorph.spi.Backend
import scala.concurrent.{ExecutionContext, Future}

case object DefaultHttpServer {

  /**
   * Default server request handler type.
   *
   * @tparam Effect effec type
   */
  type Handler[Effect[_]] = DefaultHandler.Type[Effect, UndertowServer.Context]

  /** Default server type. */
  type Type = UndertowServer

  /**
   * Creates a default server using HTTP as message transport protocol with specified ''backend'' plugin.
   *
   * The server can be used to receive and reply to requests using specific message transport protocol
   * while invoking server to process them.
   *
   * @see [[https://www.jsonrpc.org/specification protocol specification]]
   * @see [[https://undertow.io HTTP Server Documentation]]
   * @param backend effectful computation backend plugin
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
    backend: Backend[Effect],
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
   * @param executionContext execution context
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
