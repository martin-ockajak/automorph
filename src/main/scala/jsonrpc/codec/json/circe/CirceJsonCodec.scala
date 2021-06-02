package jsonrpc.codec.json.circe

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, parser}
import jsonrpc.core.EncodingOps.{asArraySeq, asString, toArraySeq}
import jsonrpc.spi.{Codec, Message, MessageError}
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline

/**
 * UPickle JSON codec plugin.
 *
 * @see [[https://circe.github.io/circe Documentation]]
 * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
 */
final case class CirceJsonCodec[EncodersDecoders](encodersDecoders: EncodersDecoders) extends Codec[Json]:
  import encodersDecoders.given

  private given Encoder[Message[Json]] = deriveEncoder[Message[Json]]
  private given Decoder[Message[Json]] = deriveDecoder[Message[Json]]

  def serialize(message: Message[Json]): ArraySeq.ofByte = message.asJson.noSpaces.toArraySeq

  def deserialize(data: ArraySeq.ofByte): Message[Json] = parser.decode[Message[Json]](data.asString).toTry.get

  def format(message: Message[Json]): String = message.asJson.spaces2

  inline def encode[T](value: T): Json =
    import encodersDecoders.given
    val encoder = summonInline[Encoder[T]]
    value.asJson(using encoder)
//    CirceJsonMacros.encode[EncodersDecoders, T](encodersDecoders, value)

  inline def decode[T](node: Json): T =
    import encodersDecoders.given
    val decoder = summonInline[Decoder[T]]
    node.as[T](using decoder).toTry.get
//    CirceJsonMacros.decode[EncodersDecoders, T](encodersDecoders, node)
