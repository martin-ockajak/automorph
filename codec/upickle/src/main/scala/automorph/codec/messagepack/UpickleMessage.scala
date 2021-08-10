package automorph.codec.messagepack

import automorph.protocol.jsonrpc.{Message, MessageError}
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
  jsonrpc: Option[String],
  id: Option[Either[BigDecimal, String]],
  method: Option[String],
  params: Option[Either[List[Msg], Map[String, Msg]]],
  result: Option[Msg],
  error: Option[UpickleMessageError]
) {

  def toProtocol: Message[Msg] = Message[Msg](
    jsonrpc,
    id,
    method,
    params,
    result,
    error.map(_.toProtocol)
  )
}

private[automorph] object UpickleMessage {

  def fromProtocol(v: Message[Msg]): UpickleMessage = UpickleMessage(
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
  data: Option[Msg]
) {

  def toProtocol: MessageError[Msg] = MessageError[Msg](
    message,
    code,
    data
  )
}

private[automorph] object UpickleMessageError {

  def fromProtocol(v: MessageError[Msg]): UpickleMessageError = UpickleMessageError(
    v.message,
    v.code,
    v.data
  )
}
