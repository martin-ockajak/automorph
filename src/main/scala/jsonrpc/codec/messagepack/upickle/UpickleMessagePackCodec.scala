package jsonrpc.codec.messagepack.upickle

import jsonrpc.core.ScalaSupport.asArraySeq
import jsonrpc.spi
import jsonrpc.spi.{Codec, Message}
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline
import upack.Msg
import upickle.Api

/**
 * UPickle MessagePack codec plugin.
 *
 * Documentation: https://github.com/com-lihaoyi/upickle
 * Effect type: Msg
 * Effect type API: http://com-lihaoyi.github.io/upickle/#uPack
 */
final case class UpickleMessagePackCodec(parser: Api)
  extends Codec[Msg]:

  private val indent = 2
  private given parser.ReadWriter[UpickleMessagePackCodec.Message] = parser.macroRW
  private given parser.ReadWriter[UpickleMessagePackCodec.CallError] = parser.macroRW

  def serialize(message: Message[Msg]): ArraySeq.ofByte =
    parser.writeToByteArray(
      UpickleMessagePackCodec.Message(message)
    ).asArraySeq

  def deserialize(data: ArraySeq.ofByte): Message[Msg] =
    parser.read[UpickleMessagePackCodec.Message](
      data.unsafeArray
    ).toSpi

  def format(message: Message[Msg]): String =
    parser.write(
      UpickleMessagePackCodec.Message(message),
      indent
    )

  inline def encode[T](value: T): Msg =
    val writer = summonInline[parser.Writer[T]]
    UpickleMessagePackMacros.encode(parser, writer, value)

  inline def decode[T](node: Msg): T =
    node.asInstanceOf[T]

object UpickleMessagePackCodec:
  final case class Message(
    jsonrpc: Option[String],
    id: Option[Either[BigDecimal, String]],
    method: Option[String],
    params: Option[Either[List[Msg], Map[String, Msg]]],
    result: Option[Msg],
    error: Option[CallError]
  ):
    def toSpi: spi.Message[Msg] =
      spi.Message[Msg](
        jsonrpc,
        id,
        method,
        params,
        result,
        error.map(_.toSpi)
      )

  object Message:
    def apply(v: spi.Message[Msg]): Message =
      Message(
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
    data: Option[Msg]
  ):
    def toSpi: spi.CallError[Msg] =
      spi.CallError[Msg](
        code,
        message,
        data
      )

  object CallError:
    def apply(v: spi.CallError[Msg]): CallError = CallError(
      v.code,
      v.message,
      v.data
    )
