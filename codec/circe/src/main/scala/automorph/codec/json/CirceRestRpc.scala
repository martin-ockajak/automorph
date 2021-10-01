package automorph.codec.json

import automorph.protocol.restrpc.{Message, MessageError}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}

/** REST-RPC protocol support for Circe message codec plugin. */
private[automorph] object CirceRestRpc {
  type Data = Message[Json]

  def messageEncoder: Encoder[Message[Json]] = {
    implicit val messageErrorEncoder: Encoder[MessageError[Json]] = deriveEncoder[MessageError[Json]]

    deriveEncoder[Message[Json]]
  }

  def messageDecoder: Decoder[Message[Json]] = {
    implicit val messageErrorDecoder: Decoder[MessageError[Json]] = deriveDecoder[MessageError[Json]]

    deriveDecoder[Message[Json]]
  }
}
