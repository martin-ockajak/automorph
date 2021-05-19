package jsonrpc.format.json.upickle

import jsonrpc.spi.{FormatContext, Message}
import jsonrpc.spi
import ujson.Value
import upickle.Api
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline

final case class UpickleJsonFormat(parser: Api)
  extends FormatContext[Value]:
  type Json = Value

  private val indent = 2
  private given parser.ReadWriter[UpickleJsonFormat.Message] = parser.macroRW
  private given parser.ReadWriter[UpickleJsonFormat.CallError] = parser.macroRW

  def serialize(message: Message[Json]): ArraySeq.ofByte =
    ArraySeq.ofByte(parser.writeToByteArray(UpickleJsonFormat.Message(message)))

  def derialize(json: ArraySeq.ofByte): Message[Json] =
    parser.read[UpickleJsonFormat.Message](json.unsafeArray).toSpi

  def format(message: Message[Json]): String =
    parser.write(UpickleJsonFormat.Message(message), indent)

  inline def encode[T](value: T): Json =
    val writer = summonInline[parser.Writer[T]]
    UpickleMacros.encode(parser, writer, value)

  inline def decode[T](json: Json): T =
    json.asInstanceOf[T]

object UpickleJsonFormat:
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

    def apply(v: spi.CallError[Json]): CallError = CallError(
      v.code,
      v.message,
      v.data
    )
  end CallError
