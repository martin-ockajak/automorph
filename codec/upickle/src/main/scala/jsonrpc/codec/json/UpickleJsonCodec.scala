package jsonrpc.codec.json

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec.{Message, MessageError, fromSpi}
import jsonrpc.spi
import scala.collection.immutable.ArraySeq
import ujson.Value

/**
 * UPickle JSON codec plugin.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[http://com-lihaoyi.github.io/upickle/#uJson Node type]]
 * @param custom customized Upickle reader and writer implicits instance
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
final case class UpickleJsonCodec[Custom <: UpickleCustom](
  custom: Custom = new UpickleCustom {}
) extends UpickleJsonCodecMeta[Custom] {

  private val indent = 2
  private implicit val messageErrorRw: custom.ReadWriter[MessageError] = custom.macroRW
  private implicit val messageRw: custom.ReadWriter[Message] = custom.macroRW

  override def mediaType: String = "application/json"

  override def serialize(message: spi.Message[Value]): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeToByteArray(fromSpi(message)))

  override def deserialize(data: ArraySeq.ofByte): spi.Message[Value] =
    custom.read[Message](data.unsafeArray).toSpi

  override def format(message: spi.Message[Value]): String =
    custom.write(fromSpi(message), indent)
}

case object UpickleJsonCodec {

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
  ) {

    def toSpi: spi.Message[Value] = spi.Message[Value](
      jsonrpc,
      id,
      method,
      params,
      result,
      error.map(_.toSpi)
    )
  }

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
  ) {

    def toSpi: spi.MessageError[Value] = spi.MessageError[Value](
      code,
      message,
      data
    )
  }

  def fromSpi(v: spi.MessageError[Value]): MessageError = MessageError(
    v.code,
    v.message,
    v.data
  )
}
