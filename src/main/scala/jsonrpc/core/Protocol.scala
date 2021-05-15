package jsonrpc.core

import java.io.IOException

trait Protocol[JsonValue]:
  type Id = Either[String, BigDecimal]

  case class Request[JsonValue](
    jsonrpc: String,
    id: Option[Id],
    method: String,
    params: Either[List[JsonValue], Map[String, JsonValue]]
  )

  case class Response[JsonValue](
    jsonrpc: String,
    id: Id,
    result: Option[JsonValue],
    error: Option[CallError[JsonValue]]
  )

  case class CallError[JsonValue](
    code: Int,
    message: String,
    data: Option[JsonValue]
  )

  enum ErrorType(val code: Int):
    case ParseError extends ErrorType(-32700)
    case InvalidRequest extends ErrorType(-32600)
    case MethodNotFound extends ErrorType(-32601)
    case InvalidParams extends ErrorType(-32602)
    case InternalError extends ErrorType(-32603)
    case IOError extends ErrorType(-32000)
    case ApplicationError extends ErrorType(0)

  final case class ParseError(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  final case class InvalidRequest(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  final case class ApplicationError[JsonValue](
    message: String,
    data: JsonValue,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  val exceptionCodes: Map[Class[?], ErrorType] = Map(
    classOf[ParseError] -> ErrorType.ParseError,
    classOf[InvalidRequest] -> ErrorType.InvalidRequest,
    classOf[IllegalArgumentException] -> ErrorType.InvalidParams,
    classOf[IOException] -> ErrorType.IOError,
  )
