package automorph.format.messagepack

import automorph.format.messagepack.UpickleMessage
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

  import custom.*

  private val indent = 2

  implicit private lazy val customMessageRw: custom.ReadWriter[UpickleMessage] = {
    implicit val messageErrorRw: custom.ReadWriter[UpickleMessageError] = custom.macroRW
    Seq(messageErrorRw)
    custom.macroRW
  }

  implicit private lazy val messageRw: ReadWriter[Message[Msg]] = readwriter[UpickleMessage].bimap[Message[Msg]](
    UpickleMessage.fromSpi,
    _.toSpi
  )

  override def mediaType: String = "application/msgpack"

  override def serialize(message: Message[Msg]): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeToByteArray(message))
//    new ArraySeq.ofByte(custom.writeBinary(message))

  override def deserialize(data: ArraySeq.ofByte): Message[Msg] =
    custom.read[Message[Msg]](data.unsafeArray)
//    custom.readBinary[Message[Msg](data.unsafeArray)

  override def serializeNode(node: Msg): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeToByteArray(node))
//    new ArraySeq.ofByte(custom.writeBinary(node))

  override def deserializeNode(data: ArraySeq.ofByte): Msg =
    custom.read[Msg](data.unsafeArray)
//    custom.readBinary[Msg](data.unsafeArray)

  override def format(message: Message[Msg]): String =
    custom.write(message, indent)
}

case object UpickleMessagePackFormat {
  /** Message node type. */
  type Node = Msg
}
