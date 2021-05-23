package jsonrpc

import jsonrpc.core.{Protocol, Request, Response}
import jsonrpc.spi.{CallError, Codec, Effect, Message}
import jsonrpc.util.ValueOps.{asLeft, asRight, asSome}
import scala.collection.immutable.ArraySeq
import scala.util.Random
import scala.util.{Failure, Success, Try}
import jsonrpc.core.Protocol.ParseErrorException
import jsonrpc.core.Protocol.MethodNotFoundException
import jsonrpc.log.Logging
import jsonrpc.util.CannotEqual

/**
 * JSON-RPC client.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Create a new JSON-RPC client using the specified `codec` and `effect` implementations.
 * @param codec hierarchical data format codec plugin
 * @param effect computation effect system plugin
 * @param transport message transport layer
 * @tparam Node data format node representation type
 * @tparam Outcome computation outcome effect type
 * @tparam Context request context type
 */
final case class JsonRpcClient[Node, Outcome[_], Context](
  codec: Codec[Node],
  effect: Effect[Outcome],
  transport: JsonRpcTransport[Outcome, Context]
) extends CannotEqual with Logging:

  private lazy val random = new Random(System.currentTimeMillis() + Runtime.getRuntime.totalMemory())

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * @param method method name
   * @param arguments arguments by position
   * @tparam R result type
   * @return result value
   */
  def call[R](method: String, arguments: Seq[Any]): Outcome[R] = call(method, arguments)(using None)

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the ''arguments by name''.
   *
   * @param method method name
   * @param arguments arguments by position
   * @tparam R result type
   * @return result value
   */
  def call[R](method: String, arguments: Map[String, Any]): Outcome[R] = call(method, arguments)(using None)

  /**
   * Perform a remote JSON-RPC method call'' supplying the ''arguments by position''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by position
   * @param context JSON-RPC request context
   * @tparam R result type
   * @return result value
   */
  def call[R](method: String, arguments: Seq[Any])(using context: Option[Context]): Outcome[R] =
    rpcCall(method, encodeArguments(arguments), context)

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the ''arguments by name''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by position
   * @param context JSON-RPC request context
   * @tparam R result type
   * @return result value
   */
  def call[R](method: String, arguments: Map[String, Any])(using context: Option[Context]): Outcome[R] =
    rpcCall(method, encodeArguments(arguments), context)

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the ''arguments by position''.
   *
   * @param method method name
   * @param arguments arguments by position
   * @tparam R result type
   * @return nothing
   */
  def notify(method: String, arguments: Seq[Any]): Outcome[Unit] = notify(method, arguments)(using None)

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the ''arguments by name''.
   *
   * @param method method name
   * @param arguments arguments by position
   * @tparam R result type
   * @return nothing
   */
  def notify(method: String, arguments: Map[String, Any]): Outcome[Unit] = notify(method, arguments)(using None)

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the ''arguments by position''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by position
   * @param context JSON-RPC request context
   * @tparam R result type
   * @return nothing
   */
  def notify(method: String, arguments: Seq[Any])(using context: Option[Context]): Outcome[Unit] =
    rpcNotify(method, encodeArguments(arguments), context)

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the ''arguments by name''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by position
   * @param context JSON-RPC request context
   * @tparam R result type
   * @return nothing
   */
  def notify(method: String, arguments: Map[String, Any])(using context: Option[Context]): Outcome[Unit] =
    rpcNotify(method, encodeArguments(arguments), context)

  /**
   * Create a ''transparent proxy instance'' of a remote JSON-RPC API.
   * Invocations of local proxy methods are translated into remote JSON-API calls.
   *
   * @tparam T remote API type
   * @return remote API proxy instance
   */
  def proxy[T]: T = ???

  private def encodeArguments(arguments: Seq[Any]): Request.Params[Node] = ???

  private def encodeArguments(arguments: Map[String, Any]): Request.Params[Node] = ???

  private def decodeResult[R](value: Node): R = ???

  private def decodeError(error: CallError[Node]): Throwable = ???

  /**
   * Perform a method call using specified arguments.
   *
   * Optional request context is used as a last method argument.
   *
   * @param methodName method name
   * @param arguments method arguments
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  private def rpcCall[R](method: String, arguments: Request.Params[Node], context: Option[Context]): Outcome[R] =
    val id = Math.abs(random.nextLong()).toString.asRight[BigDecimal].asSome
    val formalRequest = Request(id, method, arguments).message
    logger.debug(s"Performing JSON-RPC request", formalRequest.properties)
    effect.flatMap(
      serialize(formalRequest),
      rawRequest =>
        effect.flatMap(
          transport.call(rawRequest, context),
          rawResponse =>
            Try(codec.deserialize(rawResponse)) match
              case Success(formalResponse) =>
                logger.trace(s"Received JSON-RPC message:\n${codec.format(formalResponse)}")
                Try(Response(formalResponse)) match
                  case Success(validResponse) => validResponse.value match
                      case Left(errorNode) => raiseError(decodeError(errorNode), formalRequest)
                      case Right(resultNode) =>
                        Try(decodeResult(resultNode)) match
                          case Success(result) =>
                            logger.info(s"Performed JSON-RPC request", formalRequest.properties)
                            effect.pure(result)
                          case Failure(error) => raiseError(error, formalRequest)
                  case Failure(error) => raiseError(error, formalRequest)
              case Failure(error) => raiseError(ParseErrorException("Invalid response format", error), formalRequest)
        )
    )

  /**
   * Perform a method notification using specified arguments.
   *
   * Optional request context is used as a last method argument.
   *
   * @param methodName method name
   * @param arguments method arguments
   * @param context request context
   * @tparam R result type
   * @return nothing
   */
  private def rpcNotify(methodName: String, arguments: Request.Params[Node], context: Option[Context]): Outcome[Unit] =
    val formalRequest = Request(None, methodName, arguments).message
    effect.map(
      serialize(formalRequest),
      message => transport.notify(message, context)
    )

  /**
   * Serialize JSON-RPC message.
   *
   * @param formalMessage JSON-RPC message
   * @return serialized response
   */
  private def serialize(formalMessage: Message[Node]): Outcome[ArraySeq.ofByte] =
    logger.trace(s"Sending JSON-RPC message:\n${codec.format(formalMessage)}")
    Try(codec.serialize(formalMessage)) match
      case Success(message) => effect.pure(message)
      case Failure(error)   => raiseError(ParseErrorException("Invalid message format", error), formalMessage)

  /**
   * Create an error effect for a request.
   *
   * @param error exception
   * @param requestMessage request message
   * @tparam T effectful value type
   * @return error effect
   */
  private def raiseError[T](error: Throwable, requestMessage: Message[Node]): Outcome[T] =
    logger.error(s"Failed to perform JSON-RPC request", error, requestMessage.properties)
    effect.failed(error)
