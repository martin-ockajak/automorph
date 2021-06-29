package jsonrpc.codec.json

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.EncoderOps
import jsonrpc.spi.{Codec, Message}
import scala.compiletime.summonInline

/**
 * Circe JSON codec plugin code generation.
 *
 * @tparam Custom customized Circe encoders and decoders implicits instance type
 */
private[jsonrpc] trait CirceJsonCodecMeta[Custom <: CirceCustom] extends Codec[Json]:
  this: CirceJsonCodec[Custom] =>
  import custom._

  override inline def encode[T](value: T): Json =
    val encoder = summonInline[Encoder[T]].encoder
    value.asJson(using encoder)

  override inline def decode[T](node: Json): T =
    val decoder = summonInline[Decoder[T]].decoder
    node.as[T](using decoder).toTry.get
