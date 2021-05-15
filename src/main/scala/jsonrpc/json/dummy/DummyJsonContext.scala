package jsonrpc.json.dummy

import java.nio.charset.StandardCharsets
import jsonrpc.json.dummy.DummyJsonContext.Message
import jsonrpc.spi
import jsonrpc.spi.JsonContext

final case class DummyJsonContext() extends JsonContext[String]:
  type Json = String

  private val charset = StandardCharsets.UTF_8.nn

  def serialize(response: spi.Message[Json]): Array[Byte] = "".getBytes(charset).nn

  def derialize(json: Array[Byte]): spi.Message[Json] = Message(None, None, None, None, None, None)

  def encode[T](value: T): Json = value.toString

  def decode[T](json: Json): T = json.asInstanceOf[T]

object DummyJsonContext:
  type Json = String

  final case class Message(
    jsonrpc: Option[String],
    id: Option[Either[BigDecimal, String]],
    method: Option[String],
    params: Option[Either[List[Json], Map[String, Json]]],
    result: Option[Json],
    error: Option[CallError]
  ) extends spi.Message[Json]

  final case class CallError(
    code: Option[Int],
    message: Option[String],
    data: Option[Json]
  ) extends spi.CallError[Json]
