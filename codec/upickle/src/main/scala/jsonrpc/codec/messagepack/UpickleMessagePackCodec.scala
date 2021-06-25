package jsonrpc.codec.messagepack

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.messagepack.UpickleMessagePackCodec.{Message, MessageError, fromSpi}
import jsonrpc.spi
import scala.collection.immutable.ArraySeq
import upack.Msg

/**
 * UPickle MessagePack codec plugin.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[http://com-lihaoyi.github.io/upickle/#uPack Node type]]
 * @param custom customized Upickle reader and writer implicits instance
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
final case class UpickleMessagePackCodec[Custom <: UpickleCustom](
  custom: Custom = new UpickleCustom {}
) extends UpickleMessagePackCodecMeta[Custom] {

  private val indent = 2
  private implicit val messageErrorRw: custom.ReadWriter[MessageError] = custom.macroRW
  private implicit val messageRw: custom.ReadWriter[Message] = custom.macroRW

  override def mediaType: String = "application/msgpack"

  override def serialize(message: spi.Message[Msg]): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeToByteArray(fromSpi(message)))

  override def deserialize(data: ArraySeq.ofByte): spi.Message[Msg] =
    custom.read[Message](data.unsafeArray).toSpi

  override def format(message: spi.Message[Msg]): String =
    custom.write(fromSpi(message), indent)
}

case object UpickleMessagePackCodec {

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
  ) {

    def toSpi: spi.Message[Msg] = spi.Message[Msg](
      jsonrpc,
      id,
      method,
      params,
      result,
      error.map(_.toSpi)
    )
  }

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
  ) {

    def toSpi: spi.MessageError[Msg] = spi.MessageError[Msg](
      code,
      message,
      data
    )
  }

  def fromSpi(v: spi.MessageError[Msg]): MessageError = MessageError(
    v.code,
    v.message,
    v.data
  )
}
