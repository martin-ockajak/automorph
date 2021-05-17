package jsonrpc.core

import java.io.IOException
import jsonrpc.spi.{CallError, Message}

object Protocol:
  type Id = Either[BigDecimal, String]

  final case class Request[JsonValue](
    id: Option[Id],
    method: String,
    params: Either[List[JsonValue], Map[String, JsonValue]]
  )

  final case class Response[JsonValue](
    id: Id,
    value: Either[CallError[JsonValue], JsonValue]
  )

  final case class ParseError(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  final case class InvalidRequest(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  enum ErrorType(val code: Int):
    case ParseError       extends ErrorType(-32700)
    case InvalidRequest   extends ErrorType(-32600)
    case MethodNotFound   extends ErrorType(-32601)
    case InvalidParams    extends ErrorType(-32602)
    case InternalError    extends ErrorType(-32603)
    case IOError          extends ErrorType(-32000)
    case ApplicationError extends ErrorType(0)

  object Request:
    def apply[JsonValue](message: Message[JsonValue]): Request[JsonValue] =
      val jsonrpc = mandatory(message.jsonrpc, "jsonrpc")
      require(jsonrpc == version, s"Invalid JSON-RPC protocol version: $jsonrpc")
      val id = message.id
      val method = mandatory(message.method, "method")
      val params = message.params.getOrElse(Right(Map.empty))
      Request(id, method, params)

  object Response:
    def apply[JsonValue](message: Message[JsonValue]): Response[JsonValue] =
      val jsonrpc = mandatory(message.jsonrpc, "jsonrpc")
      require(jsonrpc == version, s"Invalid JSON-RPC protocol version: $jsonrpc")
      val id = mandatory(message.id, "id")
      message.result.map {
        result => Response(id, Right(result))
      }.getOrElse {
        val error = mandatory(message.error, "error")
        Response(id, Left(error))
      }

  lazy val exceptionErrorTypes: Map[Class[?], ErrorType] =
    Map(
      classOf[ParseError]               -> ErrorType.ParseError,
      classOf[InvalidRequest]           -> ErrorType.InvalidRequest,
      classOf[IllegalArgumentException] -> ErrorType.InvalidParams,
      classOf[IOException]              -> ErrorType.IOError
    )

  private val version = "2.0"

  private def mandatory[T](value: Option[T], name: String): T =
    require(
      value.isDefined,
      s"missing message property: $name"
    )
    value.get