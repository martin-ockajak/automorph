package jsonrpc.codec.json.circe

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, parser}
import jsonrpc.util.EncodingOps.{asString, toArraySeq}
import jsonrpc.spi.{Codec, Message, MessageError}
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline

/**
 * Circe JSON codec plugin.
 *
 * @see [[https://circe.github.io/circe Documentation]]
 * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
 * @param codecs Circe encoders and decoders implicits instance
 * @tparam Codecs Circe encoders and decoders implicits instance type
 */
final case class CirceJsonCodec[Codecs <: CirceCodecs](codecs: Codecs) extends Codec[Json]:

  private given Encoder[Message[Json]] = deriveEncoder[Message[Json]]
  private given Decoder[Message[Json]] = deriveDecoder[Message[Json]]

  override def mimeType: String = "application/json"

  override def serialize(message: Message[Json]): ArraySeq.ofByte =
    message.asJson.noSpaces.toArraySeq

  override def deserialize(data: ArraySeq.ofByte): Message[Json] =
    parser.decode[Message[Json]](data.asString).toTry.get

  override def format(message: Message[Json]): String =
    message.asJson.spaces2

  override inline def encode[T](value: T): Json =
    val encoder = summonInline[codecs.CirceEncoder[T]].encoder
    value.asJson(using encoder)

  override inline def decode[T](node: Json): T =
    val decoder = summonInline[codecs.CirceDecoder[T]].decoder
    node.as[T](using decoder).toTry.get

trait CirceCodecs:
  final case class CirceEncoder[T](encoder: Encoder[T])
  final case class CirceDecoder[T](decoder: Decoder[T])

  given [T]: Conversion[Encoder[T], CirceEncoder[T]] = CirceEncoder(_)
  given [T]: Conversion[Decoder[T], CirceDecoder[T]] = CirceDecoder(_)
