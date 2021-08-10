package automorph.codec.json

import automorph.protocol.restrpc.{Message, MessageError}
import ujson.Value

// Workaround for upickle bug causing the following error when using its
// macroRW method to generate readers and writers for case classes with type parameters:
//   java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
//    at upickle.implicits.CaseClassReaderPiece$$anon$1.visitEnd(CaseClassReader.scala:30)
//    at ujson.ByteParser.liftedTree1$1(ByteParser.scala:496)
//    at ujson.ByteParser.tryCloseCollection(ByteParser.scala:496)
//    at ujson.ByteParser.parseNested(ByteParser.scala:462)
//    at ujson.ByteParser.parseTopLevel0(ByteParser.scala:323)
private[automorph] final case class UpickleRestRpcMessage(
  result: Option[Value],
  error: Option[UpickleRestRpcMessageError]
) {

  def toProtocol: Message[Value] = Message[Value](
    result,
    error.map(_.toProtocol)
  )
}

private[automorph] object UpickleRestRpcMessage {

  def fromProtocol(v: Message[Value]): UpickleRestRpcMessage = UpickleRestRpcMessage(
    v.result,
    v.error.map(UpickleRestRpcMessageError.fromProtocol)
  )
}

private[automorph] final case class UpickleRestRpcMessageError(
  message: Option[String],
  code: Option[Int],
  details: Option[Value]
) {

  def toProtocol: MessageError[Value] = MessageError[Value](
    message,
    code,
    details
  )
}

private[automorph] object UpickleRestRpcMessageError {

  def fromProtocol(v: MessageError[Value]): UpickleRestRpcMessageError = UpickleRestRpcMessageError(
    v.message,
    v.code,
    v.details
  )
}