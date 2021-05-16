package jsonrpc.spi

final case class Message[Json](
  jsonrpc: Option[String],
  id: Option[Either[BigDecimal, String]],
  method: Option[String],
  params: Option[Either[List[Json], Map[String, Json]]],
  result: Option[Json],
  error: Option[CallError[Json]]
)

final case class CallError[Json](
  code: Option[Int],
  message: Option[String],
  data: Option[Json]
)
