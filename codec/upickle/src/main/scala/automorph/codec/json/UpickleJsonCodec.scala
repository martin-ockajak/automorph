package automorph.codec.json

import scala.collection.immutable.ArraySeq
import ujson.Value

/**
 * uPickle message codec plugin using JSON format.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[https://com-lihaoyi.github.io/upickle/#uJson Node type]]
 * @constructor Creates an uPickle codec plugin using JSON as message format.
 * @param custom customized Upickle reader and writer implicits instance
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
final case class UpickleJsonCodec[Custom <: UpickleJsonCustom](
  custom: Custom = UpickleJsonCustom.default
) extends UpickleJsonMeta[Custom] {

  import custom._

  private val indent = 2

  override def mediaType: String = "application/json"

  override def serialize(node: Value): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeToByteArray(node))

  override def deserialize(data: ArraySeq.ofByte): Value =
    custom.read[Value](data.unsafeArray)

  override def text(node: Value): String =
    custom.write(node, indent)
}

object UpickleJsonCodec {
  /** Message node type. */
  type Node = Value
}
