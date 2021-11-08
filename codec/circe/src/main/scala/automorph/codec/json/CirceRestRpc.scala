package automorph.codec.json

import automorph.protocol.restrpc.{Message, MessageError}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}

/**
 * REST-RPC protocol support for Circe message codec plugin using JSON format.
 */
private[automorph] object CirceRestRpc {
  type RpcMessage = Message[Json]

  lazy val messageEncoder: Encoder[Message[Json]] = {
    implicit val messageErrorEncoder: Encoder[MessageError[Json]] = deriveEncoder[MessageError[Json]]

    deriveEncoder[Message[Json]]
  }

  lazy val messageDecoder: Decoder[Message[Json]] = {
    implicit val messageErrorDecoder: Decoder[MessageError[Json]] = deriveDecoder[MessageError[Json]]

    deriveDecoder[Message[Json]]
  }
}
