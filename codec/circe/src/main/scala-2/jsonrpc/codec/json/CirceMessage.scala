package jsonrpc.codec.messagepack

import jsonrpc.spi.{Message, MessageError}
import io.circe.Json

private[jsonrpc] final case class CirceMessage(
  jsonrpc: Option[String],
  id: Option[Either[BigDecimal, String]],
  method: Option[String],
  params: Option[Either[List[Json], Map[String, Json]]],
  result: Option[Json],
  error: Option[MessagePackMessageError]
) {

  def toSpi: Message[Json] = Message[Json](
    jsonrpc,
    id,
    method,
    params,
    result,
    error.map(_.toSpi)
  )
}

private[jsonrpc] object CirceMessage {

  def fromSpi(v: Message[Json]): CirceMessage = CirceMessage(
    v.jsonrpc,
    v.id,
    v.method,
    v.params,
    v.result,
    v.error.map(MessagePackMessageError.fromSpi)
  )
}

private[jsonrpc] final case class CirceMessageError(
  code: Option[Int],
  message: Option[String],
  data: Option[Json]
) {

  def toSpi: MessageError[Json] = MessageError[Json](
    code,
    message,
    data
  )
}

private[jsonrpc] object CirceMessageError {

  def fromSpi(v: MessageError[Json]): CirceMessageError = CirceMessageError(
    v.code,
    v.message,
    v.data
  )
}
