package jsonrpc.codec.json

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.spi.Codec
import scala.compiletime.summonInline
import ujson.Value

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
private[jsonrpc] trait UpickleJsonCodecMeta[Custom <: UpickleCustom] extends Codec[Value]:
  this: UpickleJsonCodec[Custom] =>

  override inline def encode[T](value: T): Value =
    val writer = summonInline[custom.Writer[T]]
    custom.writeJs(value)(using writer)

  override inline def decode[T](node: Value): T =
    val reader = summonInline[custom.Reader[T]]
    custom.read[T](node)(using reader)
