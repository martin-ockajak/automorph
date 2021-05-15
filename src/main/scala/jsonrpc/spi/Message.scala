package jsonrpc.spi

import java.io.IOException

object Message:
  type Id = Either[String, BigDecimal]

  case class Request[JsonValue](
    jsonrpc: Option[String],
    id: Option[Id],
    method: Option[String],
    params: Option[Either[List[JsonValue], Map[String, JsonValue]]]
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
