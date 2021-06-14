package jsonrpc.codec.messagepack

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.spi.Codec
import scala.compiletime.summonInline
import upack.Msg

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom custom Upickle reader and writer implicits instance type
 */
private[jsonrpc] trait UpickleMessagePackCodecMeta[Custom <: UpickleCustom] extends Codec[Msg]:
  this: UpickleMessagePackCodec[Custom] =>

  override inline def encode[T](value: T): Msg =
    val writer = summonInline[custom.Writer[T]]
    custom.writeMsg(value)(using writer)

  override inline def decode[T](node: Msg): T =
    val reader = summonInline[custom.Reader[T]]
    custom.readBinary[T](node)(using reader)
