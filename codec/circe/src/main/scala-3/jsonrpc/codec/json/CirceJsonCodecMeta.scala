package jsonrpc.codec.json

import io.circe.syntax.EncoderOps
import io.circe.Json
import jsonrpc.spi.Codec
import scala.compiletime.summonInline

/**
 * Circe JSON codec plugin code generation.
 *
 * @tparam Custom customized Circe encoders and decoders implicits instance type
 */
trait CirceJsonCodecMeta[Custom <: CirceCustom] extends Codec[Json]:
  this: CirceJsonCodec[Custom] =>

  override inline def encode[T](value: T): Json =
    val encoder = summonInline[custom.CirceEncoder[T]].encoder
    value.asJson(using encoder)

  override inline def decode[T](node: Json): T =
    val decoder = summonInline[custom.CirceDecoder[T]].decoder
    node.as[T](using decoder).toTry.get
