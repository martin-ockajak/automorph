package jsonrpc.codec.messagepack

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.spi.Codec
import upack.Msg

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
private[jsonrpc] trait UpickleMessagePackCodecMeta[Custom <: UpickleCustom] extends Codec[Msg] {
  this: UpickleMessagePackCodec[Custom] =>

  override def encode[T](value: T): Msg = UpickleMessagePackCodecMacros.encode(custom, value)

  override def decode[T](node: Msg): T = UpickleMessagePackCodecMacros.decode(custom, node)
}
