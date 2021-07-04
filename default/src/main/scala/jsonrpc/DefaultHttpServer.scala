package jsonrpc

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import jsonrpc.Defaults.DefaultHandler
import jsonrpc.backend.IdentityBackend.Identity
import jsonrpc.server.http.UndertowJsonRpcHandler.defaultErrorStatus
import jsonrpc.server.http.UndertowServer.defaultBuilder
import jsonrpc.server.http.{UndertowJsonRpcHandler, UndertowServer}
import jsonrpc.spi.Backend
import scala.concurrent.{ExecutionContext, Future}

case object DefaultHttpServer {

  type BindApis[Effect[_]] = DefaultHandler[Effect, HttpServerExchange] => DefaultHandler[Effect, HttpServerExchange]

  /**
   * Create a JSON-RPC server using the specified ''backend'' plugin.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://undertow.io HTTP Server Documentation]]
   * @param backend effect backend plugin
   * @param runEffect effect execution function
   * @param bindApis function to bind APIs to the underlying JSON-RPC handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP handler URL path
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def apply[Effect[_]](
    backend: Backend[Effect],
    runEffect: Effect[Any] => Unit,
    bindApis: BindApis[Effect],
    port: Int = 8080,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  ): UndertowServer =
    val handler = bindApis(DefaultHandler(backend))
    UndertowServer(UndertowJsonRpcHandler(handler, runEffect, errorStatus), port, urlPath, builder)

  /**
   * Create an asynchonous JSON-RPC request handler with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param bindApis function to bind APIs to the underlying JSON-RPC handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP handler URL path
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @param executionContext execution context
   * @return asynchronous JSON-RPC request handler
   */
  def async(
    bindApis: BindApis[Future],
    port: Int = 8080,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  )(implicit executionContext: ExecutionContext): UndertowServer = {
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
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param bindApis function to bind APIs to the underlying JSON-RPC handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP handler URL path
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @param executionContext execution context
   * @return synchronous JSON-RPC request handler
   */
  def sync(
    bindApis: BindApis[Identity],
    port: Int = 8080,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  ): UndertowServer = {
    val handler = bindApis(DefaultHandler.sync())
    val runEffect = (_: Identity[Any]) => ()
    UndertowServer(UndertowJsonRpcHandler(handler, runEffect, errorStatus), port, urlPath, builder)
  }
}
