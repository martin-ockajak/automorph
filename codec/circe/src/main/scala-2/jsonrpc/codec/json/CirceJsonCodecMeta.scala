package jsonrpc.codec.json

import io.circe.Json
import jsonrpc.spi.Codec

/**
 * Circe JSON codec plugin code generation.
 *
 * @tparam Custom customized Circe encoders and decoders implicits instance type
 */
private[jsonrpc] trait CirceJsonCodecMeta[Custom <: CirceCustom] extends Codec[Json] {
  this: CirceJsonCodec[Custom] =>

  override def encode[T](value: T): Json = CirceJsonCodecMacros.encode(custom, value)

  override def decode[T](node: Json): T = CirceJsonCodecMacros.decode(custom, node)
}
