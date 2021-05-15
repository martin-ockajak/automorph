package jsonrpc

final case class ApplicationError[JsonValue](
  message: String,
  data: JsonValue,
  cause: Throwable
) extends RuntimeException(message, cause)
