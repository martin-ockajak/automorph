package jsonrpc.codec.json

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{parser, Decoder, Encoder, Json}
import java.nio.charset.StandardCharsets
import jsonrpc.spi.{Message, MessageError}
import scala.collection.immutable.ArraySeq

/**
 * Circe JSON codec plugin.
 *
 * @see [[https://circe.github.io/circe Documentation]]
 * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
 */
final case class CirceJsonCodec() extends CirceJsonCodecMeta {

  private val charset = StandardCharsets.UTF_8

  implicit private val idEncoder: Encoder[Either[BigDecimal, String]] = deriveEncoder[Either[BigDecimal, String]]
  implicit private val idDecoder: Decoder[Either[BigDecimal, String]] = deriveDecoder[Either[BigDecimal, String]]

  implicit private val paramsEncoder: Encoder[Either[List[Json], Map[String, Json]]] =
    deriveEncoder[Either[List[Json], Map[String, Json]]]

  implicit private val paramsDecoder: Decoder[Either[List[Json], Map[String, Json]]] =
    deriveDecoder[Either[List[Json], Map[String, Json]]]
  implicit private val messageErrorEncoder: Encoder[MessageError[Json]] = deriveEncoder[MessageError[Json]]
  implicit private val messageErrorDecoder: Decoder[MessageError[Json]] = deriveDecoder[MessageError[Json]]
  implicit private val messageEncoder: Encoder[Message[Json]] = deriveEncoder[Message[Json]]
  implicit private val messageDecoder: Decoder[Message[Json]] = deriveDecoder[Message[Json]]

  override def mediaType: String = "application/json"

  override def serialize(message: Message[Json]): ArraySeq.ofByte =
    new ArraySeq.ofByte(message.asJson.noSpaces.getBytes(charset))

  override def deserialize(data: ArraySeq.ofByte): Message[Json] =
    parser.decode[Message[Json]](new String(data.unsafeArray, charset)).toTry.get

  override def format(message: Message[Json]): String =
    message.asJson.spaces2
}
