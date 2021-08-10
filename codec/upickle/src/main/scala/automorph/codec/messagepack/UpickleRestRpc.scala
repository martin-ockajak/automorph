package automorph.codec.messagepack

import automorph.codec.UpickleCustom
import automorph.protocol.restrpc.{Message, MessageError}
import upack.Msg
import upickle.core.Abort

/**
 * JSON-RPC protocol support for uPickle message codec plugin.
 */
object UpickleRestRpc {
  type Data = Message[Msg]

  def readWriter[Custom <: UpickleCustom](custom: Custom): custom.ReadWriter[Message[Msg]] = {
    import custom._
    implicit val messageErrorRw: custom.ReadWriter[UpickleRestRpcMessageError] = custom.macroRW
    implicit val customMessageRw: custom.ReadWriter[UpickleRestRpcMessage] = custom.macroRW

    Seq(messageErrorRw, customMessageRw)
    readwriter[UpickleRestRpcMessage].bimap[Message[Msg]](
      UpickleRestRpcMessage.fromProtocol,
      _.toProtocol
    )
  }
}
