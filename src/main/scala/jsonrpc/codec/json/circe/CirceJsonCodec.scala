package jsonrpc.codec.json.circe

import io.circe.generic.semiauto
import io.circe.{Decoder, Encoder, Json, parser}
import io.circe.syntax.EncoderOps
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

  given Encoder[Message[Json]] = semiauto.deriveEncoder[Message[Json]]
  given Decoder[Message[Json]] = semiauto.deriveDecoder[Message[Json]]

//  private given pickler.ReadWriter[Message] = pickler.macroRW
//  private given pickler.ReadWriter[MessageError] = pickler.macroRW

  def serialize(message: Message[Json]): ArraySeq.ofByte = message.asJson.noSpaces.toArraySeq

  def deserialize(data: ArraySeq.ofByte): Message[Json] = parser.decode[Message[Json]](data.asString).toTry.get

  def format(message: Message[Json]): String = message.asJson.spaces2

  inline def encode[T](value: T): Json =
    val encoder = summonInline[Encoder[T]]
    value.asJson(encoder)

  inline def decode[T](node: Json): T =
    val decoder = summonInline[Decoder[T]]
    node.as[T](decoder).toTry.get
