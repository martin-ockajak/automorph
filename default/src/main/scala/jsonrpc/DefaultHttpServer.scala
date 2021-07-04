package jsonrpc

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import jsonrpc.Handler
import jsonrpc.server.http.UndertowJsonRpcHandler.defaultErrorStatus
import jsonrpc.spi.Backend
import scala.concurrent.ExecutionContext
import jsonrpc.server.http.{UndertowJsonRpcHandler, UndertowServer}
import jsonrpc.server.http.UndertowServer.defaultBuilder

case object DefaultHttpServer {

  /**
   * Create a JSON-RPC server using the specified ''backend'' plugin.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://undertow.io HTTP Server Documentation]]
   * @param backend effect backend plugin
   * @param effectRunAsync asynchronous effect execution function
   * @param urlPath HTTP handler URL path
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def apply[Effect[_]](
    backend: Backend[Effect],
    effectRunAsync: Effect[Any] => Unit,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  ): UndertowServer = {
    UndertowServer(UndertowJsonRpcHandler(DefaultHandler(backend), effectRunAsync, errorStatus), urlPath, builder)
  }

  /**
   * Create an asynchonous JSON-RPC request handler with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param urlPath HTTP handler URL path
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @param executionContext execution context
   * @return asynchronous JSON-RPC request handler
   */
  def async(
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  )(implicit executionContext: ExecutionContext): UndertowServer = {
    UndertowServer(UndertowJsonRpcHandler(DefaultHandler.async(), identity, errorStatus), urlPath, builder)
  }

  /**
   * Create a synchonous JSON-RPC request handler with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param urlPath HTTP handler URL path
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @param executionContext execution context
   * @return asynchronous JSON-RPC request handler
   */
  def sync(
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  ): UndertowServer = {
    UndertowServer(UndertowJsonRpcHandler(DefaultHandler.sync(), identity, errorStatus), urlPath, builder)
  }
}
