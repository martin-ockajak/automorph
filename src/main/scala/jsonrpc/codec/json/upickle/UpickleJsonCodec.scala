package jsonrpc.codec.json.upickle

import jsonrpc.codec.json.upickle.UpickleJsonCodec.{Message, MessageError, fromSpi}
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
final case class UpickleJsonCodec[Pickler <: Api](pickler: Pickler) extends Codec[Value]:

  private val indent = 2
  private given pickler.ReadWriter[Message] = pickler.macroRW
  private given pickler.ReadWriter[MessageError] = pickler.macroRW

  def serialize(message: spi.Message[Value]): ArraySeq.ofByte = pickler.writeToByteArray(fromSpi(message)).asArraySeq

  def deserialize(data: ArraySeq.ofByte): spi.Message[Value] = pickler.read[Message](data.unsafeArray).toSpi

  def format(message: spi.Message[Value]): String = pickler.write(fromSpi(message), indent)

  inline def encode[T](value: T): Value =
    val writer = summonInline[pickler.Writer[T]]
    pickler.writeJs(value)(using writer)

  inline def decode[T](node: Value): T =
    val reader = summonInline[pickler.Reader[T]]
    pickler.read[T](node)(using reader)

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

  def fromSpi(v: spi.Message[Value]): Message = Message(
    v.jsonrpc,
    v.id,
    v.method,
    v.params,
    v.result,
    v.error.map(fromSpi)
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

  def fromSpi(v: spi.MessageError[Value]): MessageError = MessageError(
    v.code,
    v.message,
    v.data
  )
