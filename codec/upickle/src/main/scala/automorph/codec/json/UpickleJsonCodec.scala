package automorph.codec.json

import automorph.codec.json.meta.UpickleJsonMeta
import automorph.util.Extensions.ByteArrayOps
import java.io.InputStream
import ujson.Value

/**
 * uPickle JSON message codec plugin.
 *
 * @see [[https://www.json.org Message format]]
 * @see [[https://github.com/com-lihaoyi/upickle Library documentation]]
 * @see [[https://com-lihaoyi.github.io/upickle/#uJson Node type]]
 * @constructor Creates an uPickle codec plugin using JSON as message format.
 * @param custom customized Upickle reader and writer implicits instance
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
final case class UpickleJsonCodec[Custom <: UpickleJsonCustom](
  custom: Custom = UpickleJsonCustom.default
) extends UpickleJsonMeta[Custom] {

  import custom.*

  private val indent = 2

  override val mediaType: String = "application/json"

  override def serialize(node: Value): InputStream =
    custom.writeToByteArray(node).toInputStream

  override def deserialize(data: InputStream): Value =
    custom.read[Value](data)

  override def text(node: Value): String =
    custom.write(node, indent)
}

object UpickleJsonCodec {
  /** Message node type. */
  type Node = Value
}
