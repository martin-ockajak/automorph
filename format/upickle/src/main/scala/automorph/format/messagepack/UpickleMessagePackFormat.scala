package automorph.format.messagepack

import automorph.format.{DefaultUpickleCustom, UpickleCustom}
import automorph.spi.Message
import scala.collection.immutable.ArraySeq
import upack.Msg

/**
 * uPickle message format plugin using MessagePack as message format.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[http://com-lihaoyi.github.io/upickle/#uPack Node type]]
 * @constructor Creates a uPickle format plugin using MessagePack as message format.
 * @param custom customized Upickle reader and writer implicits instance
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
final case class UpickleMessagePackFormat[Custom <: UpickleCustom](
  custom: Custom = DefaultUpickleCustom
) extends UpickleMessagePackFormatMeta[Custom] {

  import custom._

  private val indent = 2

  implicit private lazy val messageRw: custom.ReadWriter[UpickleMessage] = {
    implicit val messageErrorRw: custom.ReadWriter[UpickleMessageError] = custom.macroRW
    Seq(messageErrorRw  )
    custom.macroRW
  }

  override def mediaType: String = "application/msgpack"

  override def serialize(message: Message[Msg]): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeToByteArray(UpickleMessage.fromSpi(message)))

  override def deserialize(data: ArraySeq.ofByte): Message[Msg] =
    custom.read[UpickleMessage](data.unsafeArray).toSpi

  override def format(message: Message[Msg]): String =
    custom.write(UpickleMessage.fromSpi(message), indent)
}

case object UpickleMessagePackFormat {
  /** Message node type. */
  type Node = Msg
}
