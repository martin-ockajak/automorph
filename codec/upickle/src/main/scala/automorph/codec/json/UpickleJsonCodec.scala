package automorph.codec.json

import automorph.codec.{DefaultUpickleCustom, UpickleCustom}
import automorph.protocol.jsonrpc.Message
import scala.collection.immutable.ArraySeq
import ujson.Value

/**
 * uPickle message codec plugin using JSON as message format.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[http://com-lihaoyi.github.io/upickle/#uJson Node type]]
 * @constructor Creates an uPickle codec plugin using JSON as message codec.
 * @param custom customized Upickle reader and writer implicits instance
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
final case class UpickleJsonCodec[Custom <: UpickleCustom](
  custom: Custom = DefaultUpickleCustom
) extends UpickleJsonMeta[Custom] {

  private val indent = 2

  implicit private lazy val messageRw: custom.ReadWriter[Message[Value]] = JsonRpc.readWriter(custom)

  override def mediaType: String = "application/json"

  override def serialize(node: Value): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeToByteArray(node))

  override def deserialize(data: ArraySeq.ofByte): Value =
    custom.read[Value](data.unsafeArray)

  override def text(node: Value): String =
    custom.write(node, indent)
}

case object UpickleJsonCodec {
  /** Message node type. */
  type Node = Value
}
