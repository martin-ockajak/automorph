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

  val custom: Custom

  override inline def encode[T](value: T): Msg =
    custom.writeMsg(value)(using summonInline[custom.Writer[T]])

  override inline def decode[T](node: Msg): T =
    custom.readBinary[T](node)(using summonInline[custom.Reader[T]])
