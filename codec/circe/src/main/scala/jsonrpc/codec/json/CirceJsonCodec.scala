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
  private implicit val messageEncoder: Encoder[Message[Json]] = deriveEncoder[Message[Json]]
  private implicit val messageDecoder: Decoder[Message[Json]] = deriveDecoder[Message[Json]]

  override def mediaType: String = "application/json"

  override def serialize(message: Message[Json]): ArraySeq.ofByte =
    ArraySeq.ofByte(message.asJson.noSpaces.getBytes(charset))

  override def deserialize(data: ArraySeq.ofByte): Message[Json] =
    parser.decode[Message[Json]](new String(data.unsafeArray, charset)).toTry.get

  override def format(message: Message[Json]): String =
    message.asJson.spaces2
}
