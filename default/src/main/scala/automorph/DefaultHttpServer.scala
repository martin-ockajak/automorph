package automorph

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import automorph.DefaultTypes.{DefaultHandler, DefaultServer}
import automorph.backend.IdentityBackend.Identity
import automorph.server.http.UndertowJsonRpcHandler.defaultErrorStatus
import automorph.server.http.UndertowServer.defaultBuilder
import automorph.server.http.{UndertowJsonRpcHandler, UndertowServer}
import automorph.spi.Backend
import scala.concurrent.{ExecutionContext, Future}

case object DefaultHttpServer {

  type BindApis[Effect[_]] = DefaultHandler[Effect, HttpServerExchange] => DefaultHandler[Effect, HttpServerExchange]

  /**
   * Create a JSON-RPC server using the specified ''backend'' plugin.
   *
   * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
   * @see [[https://undertow.io HTTP Server Documentation]]
   * @param backend effect backend plugin
   * @param runEffect effect execution function
   * @param bindApis function to bind APIs to the underlying JSON-RPC handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP URL path (default: /)
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def apply[Effect[_]](
    backend: Backend[Effect],
    runEffect: Effect[Any] => Any,
    bindApis: BindApis[Effect],
    port: Int,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  ): DefaultServer = {
    val handler = bindApis(DefaultHandler(backend))
    UndertowServer(UndertowJsonRpcHandler(handler, runEffect, errorStatus), port, urlPath, builder)
  }
  /**
   * Create an asynchonous JSON-RPC request handler with defined request `Context` type.
   *
   * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
   * @param bindApis function to bind APIs to the underlying JSON-RPC handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP URL path (default: /)
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @param executionContext execution context
   * @return asynchronous JSON-RPC request handler
   */
  def async(
    bindApis: BindApis[Future],
    port: Int,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  )(implicit executionContext: ExecutionContext): DefaultServer = {
    Seq(executionContext)
    val handler = bindApis(DefaultHandler.async())
    val runEffect = (_: Future[Any]) => ()
    UndertowServer(UndertowJsonRpcHandler(handler, runEffect, errorStatus), port, urlPath, builder)
  }

  /**
   * Create a synchonous JSON-RPC request handler with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
   * @param bindApis function to bind APIs to the underlying JSON-RPC handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP URL path (default: /)
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @param executionContext execution context
   * @return synchronous JSON-RPC request handler
   */
  def sync(
    bindApis: BindApis[Identity],
    port: Int,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  ): DefaultServer = {
    val handler = bindApis(DefaultHandler.sync())
    val runEffect = (_: Identity[Any]) => ()
    UndertowServer(UndertowJsonRpcHandler(handler, runEffect, errorStatus), port, urlPath, builder)
  }
}
