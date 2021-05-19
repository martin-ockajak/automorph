package jsonrpc.codec.json.upickle

import jsonrpc.spi.{Codec, Message}
import jsonrpc.spi
import ujson.{Value as DOM}
import upickle.Api
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline

final case class UpickleJsonCodec(parser: Api)
  extends Codec[DOM]:

  private val indent = 2
  private given parser.ReadWriter[UpickleJsonCodec.Message] = parser.macroRW
  private given parser.ReadWriter[UpickleJsonCodec.CallError] = parser.macroRW

  def serialize(message: Message[DOM]): ArraySeq.ofByte =
    ArraySeq.ofByte(parser.writeToByteArray(UpickleJsonCodec.Message(message)))

  def deserialize(json: ArraySeq.ofByte): Message[DOM] =
    parser.read[UpickleJsonCodec.Message](json.unsafeArray).toSpi

  def format(message: Message[DOM]): String =
    parser.write(UpickleJsonCodec.Message(message), indent)

  inline def encode[T](value: T): DOM =
    val writer = summonInline[parser.Writer[T]]
    UpickleMacros.encode(parser, writer, value)

  inline def decode[T](json: DOM): T =
    json.asInstanceOf[T]

object UpickleJsonCodec:
  final case class Message(
    jsonrpc: Option[String],
    id: Option[Either[BigDecimal, String]],
    method: Option[String],
    params: Option[Either[List[DOM], Map[String, DOM]]],
    result: Option[DOM],
    error: Option[CallError]
  ):
    def toSpi: spi.Message[DOM] =
      spi.Message[DOM](
        jsonrpc,
        id,
        method,
        params,
        result,
        error.map(_.toSpi)
      )

  object Message:

    def apply(v: spi.Message[DOM]): Message =
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
    data: Option[DOM]
  ):
    def toSpi: spi.CallError[DOM] =
      spi.CallError[DOM](
        code,
        message,
        data
      )

  object CallError:

    def apply(v: spi.CallError[DOM]): CallError = CallError(
      v.code,
      v.message,
      v.data
    )
  end CallError
