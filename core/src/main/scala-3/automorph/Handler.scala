package automorph

import automorph.handler.HandlerCore.defaultExceptionToError
import automorph.handler.{HandlerCore, HandlerBind, HandlerBinding}
import automorph.log.Logging
import automorph.protocol.ErrorType
import automorph.spi.{EffectSystem, MessageFormat}
import automorph.util.{CannotEqual, EmptyContext}

/**
 * Automorph RPC request handler.
 *
 * Used by an RPC server to invoke bound API methods based on incoming RPC requests.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Creates a new JSON-RPC request handler with specified request `Context` type plus specified ''codec'' and ''backend'' plugins.
 * @param codec structured message format codec plugin
 * @param backend effect system plugin
 * @param exceptionToError maps an exception classs to a corresponding JSON-RPC error type
 * @tparam Node message node type
 * @tparam ActualMessageFormat message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, ActualMessageFormat <: MessageFormat[Node], Effect[_], Context](
  codec: ActualMessageFormat,
  backend: EffectSystem[Effect],
  methodBindings: Map[String, HandlerBinding[Node, Effect, Context]],
  protected val exceptionToError: Class[_ <: Throwable] => ErrorType,
  protected val encodeStrings: List[String] => Node,
  protected val encodedNone: Node
) extends HandlerCore[Node, ActualMessageFormat, Effect, Context]
  with HandlerBind[Node, ActualMessageFormat, Effect, Context]
  with CannotEqual
  with Logging

case object Handler:

  /** Handler with arbitrary codec. */
  type AnyCodec[Effect[_], Context] = Handler[_, _, Effect, Context]

  /**
   * Creates a JSON-RPC request handler with specified request `Context` type plus specified ''codec'' and ''backend'' plugins.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec structured message format codec plugin
   * @param backend effect system plugin
   * @tparam Node message node type
   * @tparam ActualMessageFormat message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  inline def apply[Node, ActualMessageFormat <: MessageFormat[Node], Effect[_], Context](
    codec: ActualMessageFormat,
    backend: EffectSystem[Effect]
  ): Handler[Node, ActualMessageFormat, Effect, Context] =
    val encodeStrings = (value: List[String]) => codec.encode[List[String]](value)
    Handler(codec, backend, Map.empty, defaultExceptionToError, encodeStrings, codec.encode(None))

  /**
   * Creates a JSON-RPC request handler with empty request context plus specified specified ''codec'' and ''backend'' plugins.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec structured message format codec plugin
   * @param backend effect system plugin
   * @tparam Node message node type
   * @tparam ActualMessageFormat message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  inline def withoutContext[Node, ActualMessageFormat <: MessageFormat[Node], Effect[_]](
    codec: ActualMessageFormat,
    backend: EffectSystem[Effect]
  ): Handler[Node, ActualMessageFormat, Effect, EmptyContext.Value] =
    val encodeStrings = (value: List[String]) => codec.encode[List[String]](value)
    Handler(codec, backend, Map.empty, defaultExceptionToError, encodeStrings, codec.encode(None))
