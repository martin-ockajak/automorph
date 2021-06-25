package jsonrpc.codec.json

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.spi.Codec
import ujson.Value

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
private[jsonrpc] trait UpickleJsonCodecMeta[Custom <: UpickleCustom] extends Codec[Value] {
  this: UpickleJsonCodec[Custom] =>

  override def encode[T](value: T): Value = UpickleJsonCodecMacros.encode(custom, value)

  override def decode[T](node: Value): T = UpickleJsonCodecMacros.decode(custom, node)
}
