package automorph

import automorph.handler.HandlerCore.defaultErrorMapping
import automorph.handler.{HandlerBind, HandlerBinding, HandlerCore}
import automorph.log.Logging
import automorph.protocol.jsonrpc.ErrorType
import automorph.protocol.jsonrpc.ErrorType.{InternalErrorException, InvalidRequestException, MethodNotFoundException, ParseErrorException}
import automorph.spi.{EffectSystem, MessageFormat}
import automorph.util.{CannotEqual, EmptyContext}
import java.io.IOException

/**
 * Automorph RPC request handler.
 *
 * Used by an RPC server to invoke bound API methods based on incoming RPC requests.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Creates a new RPC request handler with specified request `Context` type plus specified ''format'' and ''system'' plugins.
 * @param format message format plugin
 * @param system effect system plugin
 * @param exceptionToError maps an exception classs to a corresponding JSON-RPC error type
 * @tparam Node message node type
 * @tparam ActualFormat message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, ActualFormat <: MessageFormat[Node], Effect[_], Context](
  format: ActualFormat,
  system: EffectSystem[Effect],
  methodBindings: Map[String, HandlerBinding[Node, Effect, Context]],
  protected val exceptionToError: Throwable => ErrorType,
  protected val encodeStrings: List[String] => Node,
  protected val encodedNone: Node
) extends HandlerCore[Node, ActualFormat, Effect, Context]
  with HandlerBind[Node, ActualFormat, Effect, Context]
  with CannotEqual
  with Logging

case object Handler:

  /** Handler with arbitrary format. */
  type AnyFormat[Effect[_], Context] = Handler[_, _, Effect, Context]

  /**
   * Creates a RPC request handler with specified request `Context` type plus specified ''format'' and ''system'' plugins.
   *
   * The handler can be used by a RPC server to invoke bound API methods based on incoming RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param format message format plugin
   * @param system effect system plugin
   * @tparam Node message node type
   * @tparam ActualFormat message format plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC request handler
   */
  inline def apply[Node, ActualFormat <: MessageFormat[Node], Effect[_], Context](
    format: ActualFormat,
    system: EffectSystem[Effect]
  ): Handler[Node, ActualFormat, Effect, Context] =
    val encodeStrings = (value: List[String]) => format.encode[List[String]](value)
    Handler(format, system, Map.empty, defaultErrorMapping, encodeStrings, format.encode(None))

  /**
   * Creates a RPC request handler with empty request context plus specified specified ''format'' and ''system'' plugins.
   *
   * The handler can be used by a RPC server to invoke bound API methods based on incoming RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param format message format plugin
   * @param system effect system plugin
   * @tparam Node message node type
   * @tparam ActualFormat message format plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC request handler
   */
  inline def withoutContext[Node, ActualFormat <: MessageFormat[Node], Effect[_]](
    format: ActualFormat,
    system: EffectSystem[Effect]
  ): Handler[Node, ActualFormat, Effect, EmptyContext.Value] =
    val encodeStrings = (value: List[String]) => format.encode[List[String]](value)
    Handler(format, system, Map.empty, defaultErrorMapping, encodeStrings, format.encode(None))

  /**
   * Maps an exception class to a corresponding default JSON-RPC error type.
   *
   * @param exception exception class
   * @return JSON-RPC error type
   */
  def defaultErrorMapping(exception: Throwable): ErrorType = HandlerCore.defaultErrorMapping(exception)
