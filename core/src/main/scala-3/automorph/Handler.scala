package automorph

import java.io.IOException
import automorph.Handler.defaultMapException
import automorph.handler.{HandlerCore, HandlerMeta, HandlerMethod}
import automorph.log.Logging
import automorph.protocol.ErrorType
import automorph.protocol.ErrorType.{InternalErrorException, InvalidRequestException, MethodNotFoundException, ParseErrorException}
import automorph.spi.{Backend, Codec}
import automorph.util.{CannotEqual, NoContext}

/**
 * JSON-RPC request handler.
 *
 * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
 *
 * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
 * @constructor Create a new JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with defined request `Context` type.
 * @param codec message codec plugin
 * @param backend effect backend plugin
 * @param mapException mapping of exception class to JSON-RPC error type
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  codec: ExactCodec,
  backend: Backend[Effect],
  methodBindings: Map[String, HandlerMethod[Node, Effect, Context]],
  mapException: Class[_ <: Throwable] => ErrorType,
  protected val encodeStrings: List[String] => Node,
  protected val encodedNone: Node
) extends HandlerCore[Node, ExactCodec, Effect, Context]
  with HandlerMeta[Node, ExactCodec, Effect, Context]
  with CannotEqual
  with Logging

case object Handler:

  /**
   * Create a JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param backend effect backend plugin
   * @tparam Node message format node representation type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  inline def apply[Node, ExactCodec <: Codec[Node], Effect[_], Context](
    codec: ExactCodec,
    backend: Backend[Effect],
    mapException: Class[_ <: Throwable] => ErrorType = defaultMapException
  ): Handler[Node, ExactCodec, Effect, Context] =
    Handler(codec, backend, Map.empty, mapException, value => codec.encode[List[String]](value), codec.encode(None))

  /**
   * Create a JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with empty request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
   * @param codec hierarchical message codec plugin
   * @param backend effect backend plugin
   * @tparam Node message format node representation type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  inline def noContext[Node, ExactCodec <: Codec[Node], Effect[_]](
    codec: ExactCodec,
    backend: Backend[Effect],
    mapException: Class[_ <: Throwable] => ErrorType = defaultMapException
  ): Handler[Node, ExactCodec, Effect, NoContext.Value] =
    Handler(codec, backend, Map.empty, mapException, value => codec.encode[List[String]](value), codec.encode(None))

  /**
   * Mapping of exception class to JSON-RPC error type.
   *
   * @param exceptionClass exception class
   * @return JSON-RPC error type
   */
  def defaultMapException(exceptionClass: Class[_ <: Throwable]): ErrorType = Map(
    classOf[ParseErrorException] -> ErrorType.ParseError,
    classOf[InvalidRequestException] -> ErrorType.InvalidRequest,
    classOf[MethodNotFoundException] -> ErrorType.MethodNotFound,
    classOf[IllegalArgumentException] -> ErrorType.InvalidParams,
    classOf[InternalErrorException] -> ErrorType.InternalError,
    classOf[IOException] -> ErrorType.IOError
  ).withDefaultValue(ErrorType.ApplicationError).asInstanceOf[Map[Class[_ <: Throwable], ErrorType]](exceptionClass)
