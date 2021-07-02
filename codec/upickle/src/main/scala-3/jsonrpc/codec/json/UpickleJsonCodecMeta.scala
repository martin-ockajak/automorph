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

  val custom: Custom

  override inline def encode[T](value: T): Value =
    custom.writeJs(value)(using summonInline[custom.Writer[T]])

  override inline def decode[T](node: Value): T =
    custom.read[T](node)(using summonInline[custom.Reader[T]])
