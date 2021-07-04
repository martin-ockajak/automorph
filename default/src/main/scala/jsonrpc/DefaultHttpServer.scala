package jsonrpc

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import jsonrpc.server.http.UndertowJsonRpcHandler.defaultErrorStatus
import jsonrpc.server.http.UndertowServer.defaultBuilder
import jsonrpc.server.http.{UndertowJsonRpcHandler, UndertowServer}
import jsonrpc.spi.Backend
import jsonrpc.backend.IdentityBackend.Identity
import scala.concurrent.{ExecutionContext, Future}
import ujson.Value
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.codec.common.UpickleCustom

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
   * @param bindApis bind APIs to the underlying JSON-RPC handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP handler URL path
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def apply[Effect[_]](
    backend: Backend[Effect],
    effectRunAsync: Effect[Any] => Unit,
    bindApis: (Handler[Value, UpickleJsonCodec[UpickleCustom], Effect, HttpServerExchange]) => Handler[
      Value,
      UpickleJsonCodec[UpickleCustom],
      Effect,
      HttpServerExchange
    ],
    port: Int = 8080,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  ): UndertowServer =
    UndertowServer(
      UndertowJsonRpcHandler(bindApis(DefaultHandler(backend)), effectRunAsync, errorStatus),
      port,
      urlPath,
      builder
    )

  /**
   * Create an asynchonous JSON-RPC request handler with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param bindApis bind APIs to the underlying JSON-RPC handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP handler URL path
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @param executionContext execution context
   * @return asynchronous JSON-RPC request handler
   */
  def async(
    bindApis: (Handler[Value, UpickleJsonCodec[UpickleCustom], Future, HttpServerExchange]) => Handler[
      Value,
      UpickleJsonCodec[UpickleCustom],
      Future,
      HttpServerExchange
    ],
    port: Int = 8080,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  )(implicit executionContext: ExecutionContext): UndertowServer = {
    Seq(executionContext)
    UndertowServer(
      UndertowJsonRpcHandler(bindApis(DefaultHandler.async()), (_: Future[Any]) => (), errorStatus),
      port,
      urlPath,
      builder
    )
  }

  /**
   * Create a synchonous JSON-RPC request handler with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param bindApis bind APIs to the underlying JSON-RPC handler
   * @param port port to listen on for HTTP connections
   * @param urlPath HTTP handler URL path
   * @param builder Undertow web server builder
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @param executionContext execution context
   * @return synchronous JSON-RPC request handler
   */
  def sync(
    bindApis: (Handler[Value, UpickleJsonCodec[UpickleCustom], Identity, HttpServerExchange]) => Handler[
      Value,
      UpickleJsonCodec[UpickleCustom],
      Identity,
      HttpServerExchange
    ],
    port: Int = 8080,
    urlPath: String = "/",
    builder: Undertow.Builder = defaultBuilder,
    errorStatus: Int => Int = defaultErrorStatus
  ): UndertowServer =
    UndertowServer(
      UndertowJsonRpcHandler(bindApis(DefaultHandler.sync()), (_: Any) => (), errorStatus),
      port,
      urlPath,
      builder
    )
}
