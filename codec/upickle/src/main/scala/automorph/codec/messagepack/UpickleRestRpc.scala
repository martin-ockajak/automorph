package automorph.codec.messagepack

import automorph.protocol.restrpc.{Message, MessageError}
import upack.Msg

/** JSON-RPC protocol support for uPickle message codec using MessagePack format. */
private[automorph] object UpickleRestRpc {

  type RpcMessage = Message[Msg]

  // Workaround for upickle bug causing the following error when using its
  // macroRW method to generate readers and writers for case classes with type parameters:
  //   java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
  //    at upickle.implicits.CaseClassReaderPiece$$anon$1.visitEnd(CaseClassReader.scala:30)
  //    at ujson.ByteParser.liftedTree1$1(ByteParser.scala:496)
  //    at ujson.ByteParser.tryCloseCollection(ByteParser.scala:496)
  //    at ujson.ByteParser.parseNested(ByteParser.scala:462)
  //    at ujson.ByteParser.parseTopLevel0(ByteParser.scala:323)
  final private[automorph] case class UpickleMessage(
    result: Option[Msg],
    error: Option[UpickleMessageError]
  ) {

    def toProtocol: Message[Msg] = Message[Msg](
      result,
      error.map(_.toProtocol)
    )
  }

  private[automorph] object UpickleMessage {

    def fromProtocol(v: Message[Msg]): UpickleMessage = UpickleMessage(
      v.result,
      v.error.map(UpickleMessageError.fromProtocol)
    )
  }

  final private[automorph] case class UpickleMessageError(
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

  private[automorph] object UpickleMessageError {

    def fromProtocol(v: MessageError[Msg]): UpickleMessageError = UpickleMessageError(
      v.message,
      v.code,
      v.details
    )
  }

  def readWriter[Custom <: UpickleCustomMessagePack](custom: Custom): custom.ReadWriter[Message[Msg]] = {
    import custom._
    implicit val messageErrorRw: custom.ReadWriter[UpickleMessageError] = custom.macroRW
    implicit val customMessageRw: custom.ReadWriter[UpickleMessage] = custom.macroRW

    Seq(messageErrorRw, customMessageRw)
    readwriter[UpickleMessage].bimap[Message[Msg]](
      UpickleMessage.fromProtocol,
      _.toProtocol
    )
  }
}
