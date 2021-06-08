package jsonrpc.codec.json

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, parser}
import jsonrpc.spi.Message
import jsonrpc.util.EncodingOps.{asString, toArraySeq}
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

  override def mediaType: String = "application/json"

  override def serialize(message: Message[Json]): ArraySeq.ofByte =
    message.asJson.noSpaces.toArraySeq

  override def deserialize(data: ArraySeq.ofByte): Message[Json] =
    parser.decode[Message[Json]](data.asString).toTry.get

  override def format(message: Message[Json]): String =
    message.asJson.spaces2

trait CirceCustomized:

  final case class CirceEncoder[T](encoder: Encoder[T])
  final case class CirceDecoder[T](decoder: Decoder[T])

  given [T]: Conversion[Encoder[T], CirceEncoder[T]] = CirceEncoder(_)
  given [T]: Conversion[Decoder[T], CirceDecoder[T]] = CirceDecoder(_)
