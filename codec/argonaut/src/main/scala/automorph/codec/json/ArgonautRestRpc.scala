package automorph.codec.json

import argonaut.{Argonaut, CodecJson, Json}
import automorph.protocol.restrpc.{Message, MessageError}

/** REST-RPC protocol support for uPickle message codec plugin using JSON format. */
private[automorph] object ArgonautRestRpc {

  type RpcMessage = Message[Json]

  def messageCodecJson: CodecJson[Message[Json]] = {
    implicit val messageErrorCodecJson: CodecJson[MessageError] =
      Argonaut.codec2(MessageError.apply, (v: MessageError) => (v.message, v.code))(
        "message",
        "code"
      )

    Argonaut.codec2(Message.apply[Json], (v: Message[Json]) => (v.result, v.error))(
      "result",
      "error"
    )
  }
}
