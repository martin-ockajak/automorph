package automorph.codec.json

import automorph.protocol.restrpc.{Message, MessageError}
import ujson.Value

/**
 * JSON-RPC protocol support for uPickle message codec using JSON format.
 */
private[automorph] object UpickleRestRpc {
  type RpcMessage = Message[Value]

  // Workaround for upickle bug causing the following error when using its
  // macroRW method to generate readers and writers for case classes with type parameters:
  //   java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
  //    at upickle.implicits.CaseClassReaderPiece$$anon$1.visitEnd(CaseClassReader.scala:30)
  //    at ujson.ByteParser.liftedTree1$1(ByteParser.scala:496)
  //    at ujson.ByteParser.tryCloseCollection(ByteParser.scala:496)
  //    at ujson.ByteParser.parseNested(ByteParser.scala:462)
  //    at ujson.ByteParser.parseTopLevel0(ByteParser.scala:323)
  private[automorph] final case class UpickleMessage(
    result: Option[Value],
    error: Option[MessageError]
  ) {

    def toProtocol: Message[Value] = Message[Value](
      result,
      error
    )
  }

  private[automorph] object UpickleMessage {

    def fromProtocol(v: Message[Value]): UpickleMessage = UpickleMessage(
      v.result,
      v.error
    )
  }

  def readWriter[Custom <: UpickleJsonCustom](custom: Custom): custom.ReadWriter[Message[Value]] = {
    import custom._
    implicit val messageErrorRw: custom.ReadWriter[MessageError] = custom.macroRW
    implicit val customMessageRw: custom.ReadWriter[UpickleMessage] = custom.macroRW

    readwriter[UpickleMessage].bimap[Message[Value]](
      UpickleMessage.fromProtocol,
      _.toProtocol
    )
  }
}
