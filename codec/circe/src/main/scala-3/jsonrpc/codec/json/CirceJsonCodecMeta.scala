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
private[jsonrpc] trait CirceJsonCodecMeta extends Codec[Json]:

  override inline def encode[T](value: T): Json =
    value.asJson(using summonInline[Encoder[T]])

  override inline def decode[T](node: Json): T =
    node.as[T](using summonInline[Decoder[T]]).toTry.get
