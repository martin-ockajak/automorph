package jsonrpc.spi

final case class Message[JsonValue](
  jsonrpc: Option[String],
  id: Option[Either[BigDecimal, String]],
  method: Option[String],
  params: Option[Either[List[JsonValue], Map[String, JsonValue]]],
  result: Option[JsonValue],
  error: Option[CallError[JsonValue]]
)

final case class CallError[JsonValue](
  code: Option[Int],
  message: Option[String],
  data: Option[JsonValue]
)
