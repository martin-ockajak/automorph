package automorph.codec.json

import automorph.protocol.restrpc.{Message, MessageError}
import ujson.Value

/** JSON-RPC protocol support for uPickle message codec using JSON format. */
private[automorph] object UpickleRestRpc {

  type RpcMessage = Message[Value]

  def readWriter[Custom <: UpickleJsonCustom](custom: Custom): custom.ReadWriter[Message[Value]] = {
    import custom._
    
    implicit val messageErrorRw: custom.ReadWriter[MessageError] = custom.macroRW
    custom.macroRW[Message[Value]]
  }
}
