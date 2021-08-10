package automorph.codec.json

import automorph.codec.UpickleCustom
import automorph.protocol.restrpc.{Message, MessageError}
import ujson.Value
import upickle.core.Abort

/**
 * JSON-RPC protocol support for uPickle message codec plugin.
 */
object UpickleRestRpc {
  type Data = Message[Value]

  def readWriter[Custom <: UpickleCustom](custom: Custom): custom.ReadWriter[Message[Value]] = {
    import custom._
    implicit val messageErrorRw: custom.ReadWriter[UpickleRestRpcMessageError] = custom.macroRW
    implicit val customMessageRw: custom.ReadWriter[UpickleRestRpcMessage] = custom.macroRW

    readwriter[UpickleRestRpcMessage].bimap[Message[Value]](
      UpickleRestRpcMessage.fromProtocol,
      _.toProtocol
    )
  }
}
