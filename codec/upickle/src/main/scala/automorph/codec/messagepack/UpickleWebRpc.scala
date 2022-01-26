package automorph.codec.messagepack

import automorph.protocol.webrpc.{Message, MessageError}
import scala.annotation.nowarn
import upack.Msg

/** Web-RPC protocol support for uPickle message codec using MessagePack format. */
private[automorph] object UpickleWebRpc {

  type RpcMessage = Message[Msg]

  @nowarn("msg=used")
  def readWriter[Custom <: UpickleMessagePackCustom](custom: Custom): custom.ReadWriter[Message[Msg]] = {
    import custom.*

    implicit val messageErrorRw: custom.ReadWriter[MessageError] = custom.macroRW
    Seq(messageErrorRw)
    custom.macroRW[Message[Msg]]
  }
}
