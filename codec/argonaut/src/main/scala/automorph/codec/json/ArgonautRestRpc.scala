package automorph.codec.json

import argonaut.Argonaut.{StringToParseWrap, jArray, jNull, jNumber, jObject, jString}
import argonaut.{Argonaut, CodecJson, DecodeResult, Json, JsonObject}
import automorph.protocol.restrpc.{Message, MessageError}

/**
 * REST-RPC protocol support for uPickle message codec plugin.
 */
private[automorph] object ArgonautRestRpc {
  type Data = Message[Json]

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
