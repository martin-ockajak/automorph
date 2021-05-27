package jsonrpc.codec.json.upickle

import jsonrpc.core.EncodingOps.asArraySeq
import jsonrpc.spi
import jsonrpc.spi.Codec
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline
import ujson.Value
import upickle.Api

/**
 * UPickle JSON codec plugin.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[http://com-lihaoyi.github.io/upickle/#uJson Node type]]
 */
final case class UpickleJsonCodec(parser: Api) extends Codec[Value]:

  private val indent = 2
  private given parser.ReadWriter[UpickleJsonCodec.Message] = parser.macroRW
  private given parser.ReadWriter[UpickleJsonCodec.MessageError] = parser.macroRW

  def serialize(message: spi.Message[Value]): ArraySeq.ofByte =
    parser.writeToByteArray(UpickleJsonCodec.Message(message)).asArraySeq

  def deserialize(data: ArraySeq.ofByte): spi.Message[Value] = parser.read[UpickleJsonCodec.Message](data.unsafeArray).toSpi

  def format(message: spi.Message[Value]): String = parser.write(UpickleJsonCodec.Message(message), indent)

  inline def encode[T](value: T): Value = UpickleJsonMacros.encode(parser, value)

  inline def decode[T](node: Value): T = UpickleJsonMacros.decode[Api, T](parser, node)

case object UpickleJsonCodec:

  final case class Message(
    jsonrpc: Option[String],
    id: Option[Either[BigDecimal, String]],
    method: Option[String],
    params: Option[Either[List[Value], Map[String, Value]]],
    result: Option[Value],
    error: Option[MessageError]
  ):

    def toSpi: spi.Message[Value] = spi.Message[Value](
      jsonrpc,
      id,
      method,
      params,
      result,
      error.map(_.toSpi)
    )

  case object Message:

    def apply(v: spi.Message[Value]): Message = Message(
      v.jsonrpc,
      v.id,
      v.method,
      v.params,
      v.result,
      v.error.map(MessageError.apply)
    )

  final case class MessageError(
    code: Option[Int],
    message: Option[String],
    data: Option[Value]
  ):

    def toSpi: spi.MessageError[Value] = spi.MessageError[Value](
      code,
      message,
      data
    )

  case object MessageError:

    def apply(v: spi.MessageError[Value]): MessageError = MessageError(
      v.code,
      v.message,
      v.data
    )
