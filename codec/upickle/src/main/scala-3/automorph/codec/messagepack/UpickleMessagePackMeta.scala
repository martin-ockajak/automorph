package automorph.codec.messagepack

import automorph.codec.UpickleCustom
import automorph.spi.MessageCodec
import scala.compiletime.summonInline
import upack.Msg

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom custom Upickle reader and writer implicits instance type
 */
private[automorph] trait UpickleMessagePackMeta[Custom <: UpickleCustom] extends MessageCodec[Msg]:

  val custom: Custom

  override inline def encode[T](value: T): Msg =
    custom.writeMsg(value)(using summonInline[custom.Writer[T]])

  override inline def decode[T](node: Msg): T =
    custom.readBinary[T](node)(using summonInline[custom.Reader[T]])