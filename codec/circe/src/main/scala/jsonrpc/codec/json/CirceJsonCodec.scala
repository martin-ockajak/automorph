package jsonrpc.codec.json

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, parser}
import java.nio.charset.StandardCharsets
import jsonrpc.spi.Message
import scala.collection.immutable.ArraySeq

/**
 * Circe JSON codec plugin.
 *
 * @see [[https://circe.github.io/circe Documentation]]
 * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
 * @param customized customized Circe encoders and decoders implicits instance
 * @tparam Customized customized Circe encoders and decoders implicits instance type
 */
final case class CirceJsonCodec[Customized <: CirceCustomized](
  customized: Customized = new CirceCustomized {}
) extends CirceJsonCodecMeta[Customized]:
  private val charset = StandardCharsets.UTF_8

  override def mediaType: String = "application/json"

  override def serialize(message: Message[Json]): ArraySeq.ofByte =
    ArraySeq.ofByte(message.asJson.noSpaces.getBytes(charset))

  override def deserialize(data: ArraySeq.ofByte): Message[Json] =
    parser.decode[Message[Json]](new String(data.unsafeArray, charset)).toTry.get

  override def format(message: Message[Json]): String =
    message.asJson.spaces2

trait CirceCustomized:

  final case class CirceEncoder[T](encoder: Encoder[T])
  final case class CirceDecoder[T](decoder: Decoder[T])

  given [T]: Conversion[Encoder[T], CirceEncoder[T]] = CirceEncoder(_)
  given [T]: Conversion[Decoder[T], CirceDecoder[T]] = CirceDecoder(_)
