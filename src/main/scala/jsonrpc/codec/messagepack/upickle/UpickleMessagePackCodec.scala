package jsonrpc.codec.messagepack.upickle

import jsonrpc.codec.messagepack.upickle.UpickleMessagePackCodec.{fromSpi, Message, MessageError}
import jsonrpc.core.EncodingOps.asArraySeq
import jsonrpc.spi
import jsonrpc.spi.Codec
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline
import upack.Msg
import upickle.Api

/**
 * UPickle MessagePack codec plugin.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[http://com-lihaoyi.github.io/upickle/#uPack Node type]]
 */
final case class UpickleMessagePackCodec[Pickler <: Api](pickler: Pickler) extends Codec[Msg]:

  private val indent = 2
  private given pickler.ReadWriter[Message] = pickler.macroRW
  private given pickler.ReadWriter[MessageError] = pickler.macroRW

  def serialize(message: spi.Message[Msg]): ArraySeq.ofByte = pickler.writeToByteArray(fromSpi(message)).asArraySeq

  def deserialize(data: ArraySeq.ofByte): spi.Message[Msg] = pickler.read[Message](data.unsafeArray).toSpi

  def format(message: spi.Message[Msg]): String = pickler.write(fromSpi(message), indent)

  inline def encode[T](value: T): Msg = UpickleMessagePackMacros.encode(pickler, value)

  inline def decode[T](node: Msg): T = UpickleMessagePackMacros.decode[Pickler, T](pickler, node)

case object UpickleMessagePackCodec:

  final case class Message(
    jsonrpc: Option[String],
    id: Option[Either[BigDecimal, String]],
    method: Option[String],
    params: Option[Either[List[Msg], Map[String, Msg]]],
    result: Option[Msg],
    error: Option[MessageError]
  ):

    def toSpi: spi.Message[Msg] = spi.Message[Msg](
      jsonrpc,
      id,
      method,
      params,
      result,
      error.map(_.toSpi)
    )

  def fromSpi(v: spi.Message[Msg]): Message = Message(
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
    data: Option[Msg]
  ):

    def toSpi: spi.MessageError[Msg] = spi.MessageError[Msg](
      code,
      message,
      data
    )

  def fromSpi(v: spi.MessageError[Msg]): MessageError = MessageError(
    v.code,
    v.message,
    v.data
  )
