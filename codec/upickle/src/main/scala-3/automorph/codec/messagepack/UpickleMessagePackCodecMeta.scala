package automorph.codec.messagepack

import automorph.codec.common.UpickleCustom
import automorph.spi.MessageFormat
import scala.compiletime.summonInline
import upack.Msg

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom custom Upickle reader and writer implicits instance type
 */
private[automorph] trait UpickleMessagePackCodecMeta[Custom <: UpickleCustom] extends MessageFormat[Msg]:

  val custom: Custom

  override inline def encode[T](value: T): Msg =
    custom.writeMsg(value)(using summonInline[custom.Writer[T]])

  override inline def decode[T](node: Msg): T =
    custom.readBinary[T](node)(using summonInline[custom.Reader[T]])
