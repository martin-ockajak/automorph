package jsonrpc.codec.json

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{parser, Decoder, Encoder, Json}
import java.nio.charset.StandardCharsets
import jsonrpc.spi.Message
import scala.collection.immutable.ArraySeq

/**
 * Circe JSON codec plugin.
 *
 * @see [[https://circe.github.io/circe Documentation]]
 * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
 * @param custom customized Circe encoders and decoders implicits instance
 * @tparam Custom customized Circe encoders and decoders implicits instance type
 */
final case class CirceJsonCodec[Custom <: CirceCustom](
  custom: Custom = new CirceCustom {}
) extends CirceJsonCodecMeta[Custom] {

  private val charset = StandardCharsets.UTF_8
  private implicit val messageErrorEncoder: Encoder[CirceMessageError] = deriveEncoder[CirceMessageError]
  private implicit val messageErrorDecoder: Decoder[CirceMessageError] = deriveDecoder[CirceMessageError]
  private implicit val messageEncoder: Encoder[CirceMessage] = deriveEncoder[CirceMessage]
  private implicit val messageDecoder: Decoder[CirceMessage] = deriveDecoder[CirceMessage]

  override def mediaType: String = "application/json"

  override def serialize(message: Message[Json]): ArraySeq.ofByte =
    new ArraySeq.ofByte(CirceMessage.fromSpi(message).asJson.noSpaces.getBytes(charset))

  override def deserialize(data: ArraySeq.ofByte): Message[Json] =
    parser.decode[CirceMessage](new String(data.unsafeArray, charset)).toTry.get.toSpi

  override def format(message: Message[Json]): String =
    CirceMessage.fromSpi(message).asJson.spaces2
}
