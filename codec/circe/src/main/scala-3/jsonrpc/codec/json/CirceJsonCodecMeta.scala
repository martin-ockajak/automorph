package jsonrpc.codec.json

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import jsonrpc.spi.{Codec, Message, MessageError}
import scala.compiletime.summonInline

/**
 * Circe JSON codec plugin code generation.
 *
 * @tparam Customized customized Circe encoders and decoders implicits instance type
 */
trait CirceJsonCodecMeta[Customized <: CirceCustomized] extends Codec[Json]:
  this: CirceJsonCodec[Customized] =>

  given Encoder[Message[Json]] = deriveEncoder[Message[Json]]
  given Decoder[Message[Json]] = deriveDecoder[Message[Json]]

  override def mediaType: String = "application/json"

  override inline def encode[T](value: T): Json =
    val encoder = summonInline[customized.CirceEncoder[T]].encoder
    value.asJson(using encoder)

  override inline def decode[T](node: Json): T =
    val decoder = summonInline[customized.CirceDecoder[T]].decoder
    node.as[T](using decoder).toTry.get

