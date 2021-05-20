package jsonrpc.core

import java.io.IOException
import jsonrpc.spi.{CallError, Message}

/**
 * JSON-RPC protocol data structures.
 *
 * Specification: https://www.jsonrpc.org/specification
 */
object Protocol:
  /**
   * Message identifier type.
   */
  type Id = Either[BigDecimal, String]

  /**
   * JSON-RPC request.
   *
   * @param jsonrpc protocol version (must be 2.0)
   * @param id call identifier, a request without and identifier is considered to be a notification
   * @param method invoked method name
   * @param params invoked method argument values by position or by name
   * @tparam Node message node representation type
   */
  final case class Request[Node](
    id: Option[Id],
    method: String,
    params: Either[List[Node], Map[String, Node]]
  )

  /**
   * JSON-RPC call response.
   *
   * @param id call identifier
   * @param value response value, either a result or an error
   * @tparam Node message node representation type
   */
  final case class Response[Node](
    id: Id,
    value: Either[CallError[Node], Node]
  )

  /**
   * JSON-RPC parse error.
   *
   * @param message error message
   * @param cause error cause
   */
  final case class ParseError(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /**
   * JSON-RPC invalid request error.
   *
   * @param message error message
   * @param cause error cause
   */
  final case class InvalidRequest(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /**
   * JSON-RPC error types with codes.
   */
  enum ErrorType(val code: Int):
    case ParseError       extends ErrorType(-32700)
    case InvalidRequest   extends ErrorType(-32600)
    case MethodNotFound   extends ErrorType(-32601)
    case InvalidParams    extends ErrorType(-32602)
    case InternalError    extends ErrorType(-32603)
    case IOError          extends ErrorType(-32000)
    case ApplicationError extends ErrorType(0)

  /**
   * Mapping of standard exception types to JSON-RPC errors.
   */
  lazy val exceptionErrorTypes: Map[Class[?], ErrorType] = Map(
    classOf[ParseError]               -> ErrorType.ParseError,
    classOf[InvalidRequest]           -> ErrorType.InvalidRequest,
    classOf[IllegalArgumentException] -> ErrorType.InvalidParams,
    classOf[IOException]              -> ErrorType.IOError
  )

  object Request:
    def apply[Node](message: Message[Node]): Request[Node] =
      val jsonrpc = mandatory(message.jsonrpc, "jsonrpc")
      require(
        jsonrpc == version, 
        s"Invalid JSON-RPC protocol version: $jsonrpc"
      )
      val id = message.id
      val method = mandatory(message.method, "method")
      val params = message.params.getOrElse(Right(Map.empty))
      Request(id, method, params)

  object Response:
    def apply[Node](message: Message[Node]): Response[Node] =
      val jsonrpc = mandatory(message.jsonrpc, "jsonrpc")
      require(
        jsonrpc == version, 
        s"Invalid JSON-RPC protocol version: $jsonrpc"
      )
      val id = mandatory(message.id, "id")
      message.result.map {
        result => Response(id, Right(result))
      }.getOrElse {
        val error = mandatory(message.error, "error")
        Response(id, Left(error))
      }

  /**
   * Supported JSON-RPC protocol version.
   */
  private val version = "2.0"

  private def mandatory[T](value: Option[T], name: String): T =
    require(
      value.isDefined,
      s"Missing message property: $name"
    )
    value.get
