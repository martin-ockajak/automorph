package jsonrpc.codec.messagepack

import jsonrpc.codec.messagepack.UpickleMessagePackCodec.{Message, MessageError, fromSpi}
import jsonrpc.spi
import jsonrpc.util.EncodingOps.asArraySeq
import scala.collection.immutable.ArraySeq
import upack.Msg
import upickle.Api

/**
 * UPickle MessagePack codec plugin.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[http://com-lihaoyi.github.io/upickle/#uPack Node type]]
 * @param customized customized Upickle reader and writer implicits instance
 * @tparam Customized customized Upickle reader and writer implicits instance type
 */
final case class UpickleMessagePackCodec[Customized <: Api](customized: Customized)
  extends UpickleMessagePackCodecMeta[Customized]:

  private val indent = 2
  private given customized.ReadWriter[Message] = customized.macroRW
  private given customized.ReadWriter[MessageError] = customized.macroRW

  override def mediaType: String = "application/msgpack"

  override def serialize(message: spi.Message[Msg]): ArraySeq.ofByte =
    customized.writeToByteArray(fromSpi(message)).asArraySeq

  override def deserialize(data: ArraySeq.ofByte): spi.Message[Msg] =
    customized.read[Message](data.unsafeArray).toSpi

  override def format(message: spi.Message[Msg]): String =
    customized.write(fromSpi(message), indent)

case object UpickleMessagePackCodec:

  // Workaround for upickle bug causing the following error when using its
  // macroRW method to generate readers and writers for case classes with type parameters:
  //   java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
  //    at upickle.implicits.CaseClassReaderPiece$$anon$1.visitEnd(CaseClassReader.scala:30)
  //    at ujson.ByteParser.liftedTree1$1(ByteParser.scala:496)
  //    at ujson.ByteParser.tryCloseCollection(ByteParser.scala:496)
  //    at ujson.ByteParser.parseNested(ByteParser.scala:462)
  //    at ujson.ByteParser.parseTopLevel0(ByteParser.scala:323)
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
