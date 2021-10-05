package automorph.codec.messagepack

import automorph.codec.{DefaultUpickleCustom, UpickleCustom}
import scala.collection.immutable.ArraySeq
import upack.Msg

/**
 * uPickle message codec plugin using MessagePack format.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[https://com-lihaoyi.github.io/upickle/#uPack Node type]]
 * @constructor Creates a uPickle codec plugin using MessagePack as message format.
 * @param custom customized Upickle reader and writer implicits instance
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
final case class UpickleMessagePackCodec[Custom <: UpickleCustom](
  custom: Custom = DefaultUpickleCustom
) extends UpickleMessagePackMeta[Custom] {

  import custom._

  private val indent = 2

  implicit private lazy val jsonRpcMessageRw: custom.ReadWriter[UpickleJsonRpc.RpcMessage] =
    UpickleJsonRpc.readWriter(custom)

  implicit private lazy val restRpcMessageRw: custom.ReadWriter[UpickleRestRpc.RpcMessage] =
    UpickleRestRpc.readWriter(custom)
  Seq(jsonRpcMessageRw, restRpcMessageRw)

  override def mediaType: String = "application/msgpack"

  override def serialize(node: Msg): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeBinary(node))

  override def deserialize(data: ArraySeq.ofByte): Msg =
    custom.readBinary[Msg](data.unsafeArray)

  override def text(node: Msg): String =
    custom.write(node, indent)
}

object UpickleMessagePackCodec {
  /** Message node type. */
  type Node = Msg
}
