package jsonrpc.codec.json.upickle

import jsonrpc.spi.{Codec, Message}
import jsonrpc.spi
import ujson.Value
import upickle.Api
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline
import jsonrpc.core.ScalaSupport.*

final case class UpickleJsonCodec(parser: Api)
  extends Codec[Value]:

  import parser.*

  private val indent = 2
  private given ReadWriter[UpickleJsonCodec.Message] = macroRW
  private given ReadWriter[UpickleJsonCodec.CallError] = macroRW

  def serialize(message: Message[Value]): ArraySeq.ofByte =
    writeToByteArray(
      UpickleJsonCodec.Message(message)
    ).asArraySeq

  def deserialize(data: ArraySeq.ofByte): Message[Value] =
    read[UpickleJsonCodec.Message](
      data.unsafeArray
    ).toSpi

  def format(message: Message[Value]): String =
    write(
      UpickleJsonCodec.Message(message),
      indent
    )

  inline def encode[T](value: T): Value =
    val writer = summonInline[Writer[T]]
    UpickleMacros.encode(parser, writer, value)

  inline def decode[T](node: Value): T =
    node.asInstanceOf[T]

object UpickleJsonCodec:
  final case class Message(
    jsonrpc: Option[String],
    id: Option[Either[BigDecimal, String]],
    method: Option[String],
    params: Option[Either[List[Value], Map[String, Value]]],
    result: Option[Value],
    error: Option[CallError]
  ):
    def toSpi: spi.Message[Value] =
      spi.Message[Value](
        jsonrpc,
        id,
        method,
        params,
        result,
        error.map(_.toSpi)
      )

  object Message:

    def apply(v: spi.Message[Value]): Message =
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
    data: Option[Value]
  ):
    def toSpi: spi.CallError[Value] =
      spi.CallError[Value](
        code,
        message,
        data
      )

  object CallError:

    def apply(v: spi.CallError[Value]): CallError = CallError(
      v.code,
      v.message,
      v.data
    )
  end CallError
