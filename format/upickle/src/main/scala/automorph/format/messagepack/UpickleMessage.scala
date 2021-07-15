package automorph.codec.messagepack

import automorph.spi.{Message, MessageError}
import upack.Msg

// Workaround for upickle bug causing the following error when using its
// macroRW method to generate readers and writers for case classes with type parameters:
//   java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
//    at upickle.implicits.CaseClassReaderPiece$$anon$1.visitEnd(CaseClassReader.scala:30)
//    at ujson.ByteParser.liftedTree1$1(ByteParser.scala:496)
//    at ujson.ByteParser.tryCloseCollection(ByteParser.scala:496)
//    at ujson.ByteParser.parseNested(ByteParser.scala:462)
//    at ujson.ByteParser.parseTopLevel0(ByteParser.scala:323)
private[automorph] final case class UpickleMessage(
  automorph: Option[String],
  id: Option[Either[BigDecimal, String]],
  method: Option[String],
  params: Option[Either[List[Msg], Map[String, Msg]]],
  result: Option[Msg],
  error: Option[UpickleMessageError]
) {

  def toSpi: Message[Msg] = Message[Msg](
    automorph,
    id,
    method,
    params,
    result,
    error.map(_.toSpi)
  )
}

private[automorph] object UpickleMessage {

  def fromSpi(v: Message[Msg]): UpickleMessage = UpickleMessage(
    v.automorph,
    v.id,
    v.method,
    v.params,
    v.result,
    v.error.map(UpickleMessageError.fromSpi)
  )
}

private[automorph] final case class UpickleMessageError(
  code: Option[Int],
  message: Option[String],
  data: Option[Msg]
) {

  def toSpi: MessageError[Msg] = MessageError[Msg](
    code,
    message,
    data
  )
}

private[automorph] object UpickleMessageError {

  def fromSpi(v: MessageError[Msg]): UpickleMessageError = UpickleMessageError(
    v.code,
    v.message,
    v.data
  )
}