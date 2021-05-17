package jsonrpc.json.upickle

import java.nio.charset.StandardCharsets
import jsonrpc.spi.{CallError, JsonContext, Message}
import jsonrpc.spi
import ujson.Value
import upickle.default.{Writer, Reader, ReadWriter, macroRW}

final case class UpickleJsonContext()
  extends JsonContext[Value, Writer, Reader]:

  type Json = Value
  type Encoder[T] = Writer[T]
  type Decoder[T] = Reader[T]

  private val indent = 2

  def serialize(message: Message[Json]): Array[Byte] =
    upickle.default.writeToByteArray(UpickleJsonContext.Message(message))

  def derialize(json: Array[Byte]): Message[Json] =
    upickle.default.read[UpickleJsonContext.Message](json).toSpi

  def format(message: Message[Json]): String =
    upickle.default.write(UpickleJsonContext.Message(message), indent)

  def encode[T: Encoder](value: T): Json =
    upickle.default.writeJs(value)

  def decode[T: Decoder](json: Json): T =
    upickle.default.read[T](json)

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
    given ReadWriter[Message] = macroRW

    def apply(v: spi.Message[Json]): Message =
      Message(
        v.jsonrpc,
        v.id,
        v.method,
        v.params,
        v.result,
        v.error.map(CallError.apply)
      )
  end Message

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
    given ReadWriter[CallError] = macroRW

    def apply(v: spi.CallError[Json]): CallError = CallError(
      v.code,
      v.message,
      v.data
    )
  end CallError

end UpickleJsonContext