package jsonrpc.codec.json.circe

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, parser}
import jsonrpc.core.EncodingOps.{asArraySeq, asString, toArraySeq}
import jsonrpc.spi.{Codec, Message, MessageError}
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline

trait CirceCodecs:
  final case class CirceEncoder[T](underlying: Encoder[T])
  final case class CirceDecoder[T](underlying: Decoder[T])

  extension[T](encoder:Encoder[T]) def wrap = CirceEncoder(encoder)
  extension[T](decoder:Decoder[T]) def wrap = CirceDecoder(decoder)

/**
 * Circe JSON codec plugin.
 *
 * @see [[https://circe.github.io/circe Documentation]]
 * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
 * @param encodeDecoders Circe encoders and decoders implicits instance
 * @tparam Codecs Circe encoders and decoders implicits instance type
 */
final case class CirceJsonCodec[Codecs <: CirceCodecs](codecs: Codecs) extends Codec[Json]:

  private given Encoder[Message[Json]] = deriveEncoder[Message[Json]]
  private given Decoder[Message[Json]] = deriveDecoder[Message[Json]]

  def serialize(message: Message[Json]): ArraySeq.ofByte =
    message.asJson.noSpaces.toArraySeq

  def deserialize(data: ArraySeq.ofByte): Message[Json] =
    parser.decode[Message[Json]](data.asString).toTry.get

  def format(message: Message[Json]): String =
    message.asJson.spaces2

  inline def encode[T](value: T): Json =
    val encoder = summonInline[codecs.CirceEncoder[T]].underlying
    value.asJson(using encoder)
//    CirceJsonMacros.encode[EncodeDecoders, T](encodeDecoders, value)

  inline def decode[T](node: Json): T =
    val decoder = summonInline[codecs.CirceDecoder[T]].underlying
    node.as[T](using decoder).toTry.get
//    CirceJsonMacros.decode[EncodeDecoders, T](encodeDecoders, node)
