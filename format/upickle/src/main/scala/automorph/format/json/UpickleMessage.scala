package automorph.format.json

import automorph.spi.{Message, MessageError}
import ujson.Value

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
  params: Option[Either[List[Value], Map[String, Value]]],
  result: Option[Value],
  error: Option[UpickleMessageError]
) {

  def toSpi: Message[Value] = Message[Value](
    automorph,
    id,
    method,
    params,
    result,
    error.map(_.toSpi)
  )
}

private[automorph] object UpickleMessage {

  def fromSpi(v: Message[Value]): UpickleMessage = UpickleMessage(
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
  data: Option[Value]
) {

  def toSpi: MessageError[Value] = MessageError[Value](
    code,
    message,
    data
  )
}

private[automorph] object UpickleMessageError {

  def fromSpi(v: MessageError[Value]): UpickleMessageError = UpickleMessageError(
    v.code,
    v.message,
    v.data
  )
}
