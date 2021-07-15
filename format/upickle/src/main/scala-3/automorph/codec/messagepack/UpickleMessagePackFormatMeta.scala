package automorph.format.messagepack

import automorph.format.UpickleCustom
import automorph.spi.MessageFormat
import scala.compiletime.summonInline
import upack.Msg

/**
 * UPickle JSON format plugin code generation.
 *
 * @tparam Custom custom Upickle reader and writer implicits instance type
 */
private[automorph] trait UpickleMessagePackFormatMeta[Custom <: UpickleCustom] extends MessageFormat[Msg]:

  val custom: Custom

  override inline def encode[T](value: T): Msg =
    custom.writeMsg(value)(using summonInline[custom.Writer[T]])

  override inline def decode[T](node: Msg): T =
    custom.readBinary[T](node)(using summonInline[custom.Reader[T]])
