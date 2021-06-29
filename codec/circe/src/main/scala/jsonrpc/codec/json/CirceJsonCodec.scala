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

  implicit private val idEncoder: Encoder[Either[BigDecimal, String]] = deriveEncoder[Either[BigDecimal, String]]
  implicit private val idDecoder: Decoder[Either[BigDecimal, String]] = deriveDecoder[Either[BigDecimal, String]]

  implicit private val paramsEncoder: Encoder[Either[List[io.circe.Json], Map[String, io.circe.Json]]] =
    deriveEncoder[Either[List[io.circe.Json], Map[String, io.circe.Json]]]

  implicit private val paramsDecoder: Decoder[Either[List[io.circe.Json], Map[String, io.circe.Json]]] =
    deriveDecoder[Either[List[io.circe.Json], Map[String, io.circe.Json]]]
  implicit private val messageErrorEncoder: Encoder[CirceMessageError] = deriveEncoder[CirceMessageError]
  implicit private val messageErrorDecoder: Decoder[CirceMessageError] = deriveDecoder[CirceMessageError]
  implicit private val messageEncoder: Encoder[CirceMessage] = deriveEncoder[CirceMessage]
  implicit private val messageDecoder: Decoder[CirceMessage] = deriveDecoder[CirceMessage]

  override def mediaType: String = "application/json"

  override def serialize(message: Message[Json]): ArraySeq.ofByte =
    new ArraySeq.ofByte(CirceMessage.fromSpi(message).asJson.noSpaces.getBytes(charset))

  override def deserialize(data: ArraySeq.ofByte): Message[Json] =
    parser.decode[CirceMessage](new String(data.unsafeArray, charset)).toTry.get.toSpi

  override def format(message: Message[Json]): String =
    CirceMessage.fromSpi(message).asJson.spaces2
}
