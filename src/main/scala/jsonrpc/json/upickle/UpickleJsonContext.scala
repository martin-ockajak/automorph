package jsonrpc.json.upickle

import java.nio.charset.StandardCharsets
import jsonrpc.spi.{CallError, JsonContext, Message}
import jsonrpc.spi
import ujson.Value
import upickle.default.{ReadWriter, macroRW}

final case class UpickleJsonContext() extends JsonContext[Value]:
  type Json = Value

  private given ReadWriter[UpickleJsonContext.CallError] = macroRW
  private given ReadWriter[UpickleJsonContext.Message] = macroRW

  def encode[T](value: T): Json = ???

  def decode[T](json: Json): T = ???

  def serialize(response: Message[Json]): Array[Byte] =
    upickle.default.writeToByteArray(UpickleJsonContext.Message(response))

  def derialize(json: Array[Byte]): Message[Json] =
    upickle.default.read[UpickleJsonContext.Message](json).toSpi

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
  ):
    def toSpi: spi.Message[Json] =
      spi.Message[Json](
        jsonrpc,
        id,
        method,
        params,
        result,
        error.map(_.toSpi)
      )

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
  ):
    def toSpi: spi.CallError[Json] =
      spi.CallError[Json](
        code,
        message,
        data
      )

  object CallError:
    def apply(v: spi.CallError[Json]): CallError = CallError(
      v.code,
      v.message,
      v.data
    )
