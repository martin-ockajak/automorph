package jsonrpc.codec.json.upickle

import jsonrpc.codec.json.upickle.UpickleJsonCodec.{fromSpi, Message, MessageError}
import jsonrpc.util.EncodingOps.asArraySeq
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
 * @param readWriters Upickle reader and writer implicits instance
 * @tparam ReadWriters Upickle reader and writer implicits instance type
 */
final case class UpickleJsonCodec[ReadWriters <: Api](readWriters: ReadWriters = upickle.default) extends Codec[Value]:

  private val indent = 2
  private given readWriters.ReadWriter[Message] = readWriters.macroRW
  private given readWriters.ReadWriter[MessageError] = readWriters.macroRW

  def serialize(message: spi.Message[Value]): ArraySeq.ofByte =
    readWriters.writeToByteArray(fromSpi(message)).asArraySeq

  def deserialize(data: ArraySeq.ofByte): spi.Message[Value] =
    readWriters.read[Message](data.unsafeArray).toSpi

  def format(message: spi.Message[Value]): String =
    readWriters.write(fromSpi(message), indent)

  inline def encode[T](value: T): Value =
    val writer = summonInline[readWriters.Writer[T]]
    readWriters.writeJs(value)(using writer)

  inline def decode[T](node: Value): T =
    val reader = summonInline[readWriters.Reader[T]]
    readWriters.read[T](node)(using reader)

case object UpickleJsonCodec:

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
