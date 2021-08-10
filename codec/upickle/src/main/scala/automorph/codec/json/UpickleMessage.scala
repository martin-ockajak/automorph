package automorph.codec.json

import automorph.protocol.jsonrpc.{Message, MessageError}
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
  jsonrpc: Option[String],
  id: Option[Either[BigDecimal, String]],
  method: Option[String],
  params: Option[Either[List[Value], Map[String, Value]]],
  result: Option[Value],
  error: Option[UpickleMessageError]
) {

  def toProtocol: Message[Value] = Message[Value](
    jsonrpc,
    id,
    method,
    params,
    result,
    error.map(_.toProtocol)
  )
}

private[automorph] object UpickleMessage {

  def fromProtocol(v: Message[Value]): UpickleMessage = UpickleMessage(
    v.jsonrpc,
    v.id,
    v.method,
    v.params,
    v.result,
    v.error.map(UpickleMessageError.fromProtocol)
  )
}

private[automorph] final case class UpickleMessageError(
  message: Option[String],
  code: Option[Int],
  data: Option[Value]
) {

  def toProtocol: MessageError[Value] = MessageError[Value](
    message,
    code,
    data
  )
}

private[automorph] object UpickleMessageError {

  def fromProtocol(v: MessageError[Value]): UpickleMessageError = UpickleMessageError(
    v.message,
    v.code,
    v.data
  )
}
