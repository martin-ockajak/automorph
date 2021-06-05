package jsonrpc.codec.messagepack.upickle

import jsonrpc.codec.messagepack.upickle.UpickleMessagePackCodec.{fromSpi, Message, MessageError}
import jsonrpc.util.EncodingOps.asArraySeq
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
 * @param readWriters Upickle reader and writer implicits instance
 * @tparam ReadWriters Upickle reader and writer implicits instance type
 */
final case class UpickleMessagePackCodec[ReadWriters <: Api](readWriters: ReadWriters) extends Codec[Msg]:

  private val indent = 2
  private given readWriters.ReadWriter[Message] = readWriters.macroRW
  private given readWriters.ReadWriter[MessageError] = readWriters.macroRW

  override def mimeType: String = "application/msgpack"

  override def serialize(message: spi.Message[Msg]): ArraySeq.ofByte =
    readWriters.writeToByteArray(fromSpi(message)).asArraySeq

  override def deserialize(data: ArraySeq.ofByte): spi.Message[Msg] =
    readWriters.read[Message](data.unsafeArray).toSpi

  override def format(message: spi.Message[Msg]): String =
    readWriters.write(fromSpi(message), indent)

  override inline def encode[T](value: T): Msg =
    val writer = summonInline[readWriters.Writer[T]]
    readWriters.writeMsg(value)(using writer)

  override inline def decode[T](node: Msg): T =
    val reader = summonInline[readWriters.Reader[T]]
    readWriters.readBinary[T](node)(using reader)

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
