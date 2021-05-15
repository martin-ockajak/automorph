package jsonrpc.json.upickle

import java.nio.charset.StandardCharsets
import jsonrpc.spi
import jsonrpc.spi.JsonContext
import UpickleJsonContext.CallError
import UpickleJsonContext.Message
import upickle.default.ReadWriter
import upickle.default.macroRW
import ujson.Value

final case class UpickleJsonContext() extends JsonContext[Value]:
  type Json = Value

  private val charset = StandardCharsets.UTF_8.nn
  private given ReadWriter[CallError] = macroRW
  private given ReadWriter[Message] = macroRW

  def encode[T](value: T): Json = ???

  def decode[T](json: Json): T = ???

  def serialize(response: spi.Message[Json]): Array[Byte] =
    upickle.default.write(Message(response)).getBytes(charset).nn

  def derialize(json: Array[Byte]): spi.Message[Json] =
    upickle.default.read[Message](String(json, charset))

//  def encode[T](value: T): Json = upickle.default.writeJs(value)

//  def decode[T](json: Json): T = upickle.default.read[T](json)

object UpickleJsonContext:
  type Json = Value

  final case class Message(
    jsonrpc: Option[String],
    id: Option[Either[BigDecimal, String]],
    method: Option[String],
    params: Option[Either[List[Json], Map[String, Json]]],
    result: Option[Json],
    error: Option[CallError]
  ) extends spi.Message[Json]

  object Message:
    def apply(v: spi.Message[Json]): Message = Message(
      v.jsonrpc,
      v.id,
      v.method,
      v.params,
      v.result,
      v.error.map(CallError.apply)
    )

  final case class CallError(
    code: Option[Int],
    message: Option[String],
    data: Option[Json]
  ) extends spi.CallError[Json]

  object CallError:
    def apply(v: spi.CallError[Json]): CallError = CallError(
      v.code,
      v.message,
      v.data
    )
