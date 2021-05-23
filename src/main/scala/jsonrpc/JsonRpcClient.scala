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

  private def rpcCall[R](method: String, arguments: Request.Params[Node], context: Option[Context]): Outcome[R] =
    val id = Math.abs(random.nextLong()).toString.asRight[BigDecimal].asSome
    val requestMessage = Request(id, method, arguments).message
    logger.debug(s"Performing JSON-RPC request", requestMessage.properties)
    Try(codec.serialize(requestMessage)) match
      case Success(message) => effect.map(
        transport.call(message, context),
        message =>
          Try(codec.deserialize(message)) match
          case Success(responseMessage) =>
            logger.trace(s"Received JSON-RPC message:\n${codec.format(responseMessage)}")
            Try(Response(responseMessage)) match
              case Success(response) => response.value match
                case Left(error)   => throw decodeError(error)
                case Right(result) =>
                  logger.info(s"Performing JSON-RPC request", requestMessage.properties)
                  decodeResult(result)
              case Failure(error) => throw error
          case Failure(error) => throw ParseErrorException("Invalid response format", error)
      )
      case Failure(error) =>
        logger.error(s"Failed to perform JSON-RPC request", error, requestMessage.properties)
        effect.failed(error)

  private def rpcNotify[R](method: String, arguments: Request.Params[Node], context: Option[Context]): Outcome[Unit] =
    val requestMessage = Request(None, method, arguments).message
    serialize(requestMessage) match
      case Success(message) => transport.notify(message, context)
      case Failure(error) => effect.failed(error)

  private def serialize(requestMessage: Message[Node]): Try[ArraySeq.ofByte] =
    logger.trace(s"Sending JSON-RPC message:\n${codec.format(requestMessage)}")
    Try(codec.serialize(requestMessage)) match
      case Success(message) => Success(message)
      case Failure(error) => Failure(ParseErrorException("Invalid request format", error))
