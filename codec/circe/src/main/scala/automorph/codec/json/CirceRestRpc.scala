package automorph.codec.json

import automorph.protocol.restrpc.{Message, MessageError}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, HCursor, Json}

/** REST-RPC protocol support for Circe message codec plugin using JSON format. */
private[automorph] object CirceRestRpc {

  type RpcMessage = Message[Json]

  def messageEncoder: Encoder[Message[Json]] = {
    implicit val messageErrorEncoder: Encoder[MessageError] = deriveEncoder[MessageError]

    deriveEncoder[Message[Json]]
  }

  def messageDecoder: Decoder[Message[Json]] = {
    implicit val messageErrorDecoder: Decoder[MessageError] = deriveDecoder[MessageError]

    deriveDecoder[Message[Json]]
  }
}
