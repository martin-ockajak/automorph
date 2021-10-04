package automorph.codec.json

import argonaut.{Argonaut, CodecJson, Json}
import automorph.protocol.restrpc.{Message, MessageError}

/**
 * REST-RPC protocol support for uPickle message codec plugin using JSON format.
 */
private[automorph] object ArgonautRestRpc {
  type RpcMessage = Message[Json]

  def messageCodecJson: CodecJson[Message[Json]] = {
    implicit val messageErrorCodecJson: CodecJson[MessageError[Json]] =
      Argonaut.codec3(MessageError.apply[Json], (v: MessageError[Json]) => (v.message, v.code, v.details))(
        "message",
        "code",
        "details"
      )

    Argonaut.codec2(Message.apply[Json], (v: Message[Json]) => (v.result, v.error))(
      "result",
      "error"
    )
  }
}
