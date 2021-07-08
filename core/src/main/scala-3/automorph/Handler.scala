package automorph

import automorph.handler.HandlerCore.defaultExceptionToError
import automorph.handler.{HandlerCore, HandlerBind, HandlerBinding}
import automorph.log.Logging
import automorph.protocol.ErrorType
import automorph.spi.{Backend, Codec}
import automorph.util.{CannotEqual, NoContext}

/**
 * JSON-RPC request handler.
 *
 * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
 *
 * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
 * @constructor Creates a new JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with defined request `Context` type.
 * @param codec hierarchical message format codec plugin
 * @param backend effectful computation backend plugin
 * @param exceptionToError maps an exception classs to a corresponding JSON-RPC error type
 * @tparam Node message node type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  codec: ExactCodec,
  backend: Backend[Effect],
  methodBindings: Map[String, HandlerBinding[Node, Effect, Context]],
  protected val exceptionToError: Class[_ <: Throwable] => ErrorType,
  protected val encodeStrings: List[String] => Node,
  protected val encodedNone: Node
) extends HandlerCore[Node, ExactCodec, Effect, Context]
  with HandlerBind[Node, ExactCodec, Effect, Context]
  with CannotEqual
  with Logging

case object Handler:

  /**
   * Creates a JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
   * @param codec hierarchical message format codec plugin
   * @param backend effectful computation backend plugin
   * @tparam Node message node type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  inline def apply[Node, ExactCodec <: Codec[Node], Effect[_], Context](
    codec: ExactCodec,
    backend: Backend[Effect]
  ): Handler[Node, ExactCodec, Effect, Context] =
    val encodeStrings = (value: List[String]) => codec.encode[List[String]](value)
    Handler(codec, backend, Map.empty, defaultExceptionToError, encodeStrings, codec.encode(None))

  /**
   * Creates a JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with empty request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
   * @param codec hierarchical message format codec plugin
   * @param backend effectful computation backend plugin
   * @tparam Node message node type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  inline def noContext[Node, ExactCodec <: Codec[Node], Effect[_]](
    codec: ExactCodec,
    backend: Backend[Effect]
  ): Handler[Node, ExactCodec, Effect, NoContext.Value] =
    val encodeStrings = (value: List[String]) => codec.encode[List[String]](value)
    Handler(codec, backend, Map.empty, defaultExceptionToError, encodeStrings, codec.encode(None))
