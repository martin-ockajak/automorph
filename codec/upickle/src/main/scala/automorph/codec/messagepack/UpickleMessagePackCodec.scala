package automorph.codec.messagepack

import automorph.codec.messagepack.meta.UpickleMessagePackMeta
import automorph.util.Extensions.ByteArrayOps
import java.io.InputStream
import upack.Msg

/**
 * uPickle MessagePack message codec plugin.
 *
 * @see [[https://msgpack.org Message format]]
 * @see [[https://github.com/com-lihaoyi/upickle Library documentation]]
 * @see [[https://com-lihaoyi.github.io/upickle/#uPack Node type]]
 * @constructor Creates a uPickle codec plugin using MessagePack as message format.
 * @param custom customized Upickle reader and writer implicits instance
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
final case class UpickleMessagePackCodec[Custom <: UpickleMessagePackCustom](
  custom: Custom = UpickleMessagePackCustom.default
) extends UpickleMessagePackMeta[Custom] {

  import custom.*

  private val indent = 2

  override val mediaType: String = "application/msgpack"

  override def serialize(node: Msg): InputStream =
    custom.writeBinary(node).toInputStream

  override def deserialize(data: InputStream): Msg =
    custom.readBinary[Msg](data)

  override def text(node: Msg): String =
    custom.write(node, indent)
}

object UpickleMessagePackCodec {
  /** Message node type. */
  type Node = Msg
}
