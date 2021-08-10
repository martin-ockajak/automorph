package automorph.codec.messagepack

import automorph.protocol.restrpc.{Message, MessageError}
import upack.Msg

// Workaround for upickle bug causing the following error when using its
// macroRW method to generate readers and writers for case classes with type parameters:
//   java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
//    at upickle.implicits.CaseClassReaderPiece$$anon$1.visitEnd(CaseClassReader.scala:30)
//    at ujson.ByteParser.liftedTree1$1(ByteParser.scala:496)
//    at ujson.ByteParser.tryCloseCollection(ByteParser.scala:496)
//    at ujson.ByteParser.parseNested(ByteParser.scala:462)
//    at ujson.ByteParser.parseTopLevel0(ByteParser.scala:323)
private[automorph] final case class UpickleRestRpcMessage(
  result: Option[Msg],
  error: Option[UpickleRestRpcMessageError]
) {

  def toProtocol: Message[Msg] = Message[Msg](
    result,
    error.map(_.toProtocol)
  )
}

private[automorph] object UpickleRestRpcMessage {

  def fromProtocol(v: Message[Msg]): UpickleRestRpcMessage = UpickleRestRpcMessage(
    v.result,
    v.error.map(UpickleRestRpcMessageError.fromProtocol)
  )
}

private[automorph] final case class UpickleRestRpcMessageError(
  message: Option[String],
  code: Option[Int],
  details: Option[Msg]
) {

  def toProtocol: MessageError[Msg] = MessageError[Msg](
    message,
    code,
    details
  )
}

private[automorph] object UpickleRestRpcMessageError {

  def fromProtocol(v: MessageError[Msg]): UpickleRestRpcMessageError = UpickleRestRpcMessageError(
    v.message,
    v.code,
    v.details
  )
}
