package automorph.format.json

import automorph.format.{DefaultUpickleCustom, UpickleCustom}
import automorph.spi.Message
import scala.collection.immutable.ArraySeq
import ujson.Value

/**
 * uPickle message format plugin using JSON as message format.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[http://com-lihaoyi.github.io/upickle/#uJson Node type]]
 * @constructor Creates an uPickle format plugin using JSON as message format.
 * @param custom customized Upickle reader and writer implicits instance
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
final case class UpickleJsonFormat[Custom <: UpickleCustom](
  custom: Custom = DefaultUpickleCustom
) extends UpickleJsonMeta[Custom] {

  import custom._

  private val indent = 2

  implicit private lazy val customMessageRw: custom.ReadWriter[UpickleMessage] = {
    implicit val messageErrorRw: custom.ReadWriter[UpickleMessageError] = custom.macroRW
    custom.macroRW
  }

  implicit private lazy val messageRw: ReadWriter[Message[Value]] = readwriter[UpickleMessage].bimap[Message[Value]](
    UpickleMessage.fromSpi,
    _.toSpi
  )

  override def mediaType: String = "application/json"

  override def serialize(message: Message[Value]): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeToByteArray(message))

  override def deserialize(data: ArraySeq.ofByte): Message[Value] =
    custom.read[Message[Value]](data.unsafeArray)

  override def serializeNode(node: Value): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeToByteArray(node))

  override def deserializeNode(data: ArraySeq.ofByte): Value =
    custom.read[Value](data.unsafeArray)

  override def format(message: Message[Value]): String =
    custom.write(UpickleMessage.fromSpi(message), indent)
}

case object UpickleJsonFormat {
  /** Message node type. */
  type Node = Value
}
