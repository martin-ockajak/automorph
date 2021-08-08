package automorph.format.messagepack

import automorph.format.messagepack.UpickleMessage
import automorph.format.{DefaultUpickleCustom, UpickleCustom}
import automorph.spi.Message
import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import upack.{Arr, Float64, Msg, Obj, Str}
import upickle.core.Abort

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
) extends UpickleMessagePackMeta[Custom] {

  import custom._

  private val indent = 2

  implicit def idRw: ReadWriter[Message.Id] = readwriter[Msg].bimap[Message.Id](
    {
      case Right(id) => Str(id)
      case Left(id) => Float64(id.toDouble)
    },
    {
      case Str(id) => Right(id)
      case Float64(id) => Left(BigDecimal(id))
      case id => throw Abort(s"Invalid request identifier: $id")
    }
  )

  implicit def paramsRw: ReadWriter[Message.Params[Msg]] = readwriter[Msg].bimap[Message.Params[Msg]](
    {
      case Right(params) => Obj(mutable.LinkedHashMap[Msg, Msg](params.map { case (key, value) =>
        Str(key) -> value
      }.toSeq*))
      case Left(params) => Arr(params*)
    },
    {
      case Obj(params) => Right(params.toMap.map {
        case (Str(key), value) => key -> value
        case _ => throw Abort(s"Invalid request parameters: $params")
      })
      case Arr(params) => Left(params.toList)
      case params => throw Abort(s"Invalid request parameters: $params")
    }
  )

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
    new ArraySeq.ofByte(custom.writeBinary(message))

  override def deserialize(data: ArraySeq.ofByte): Message[Msg] =
    custom.readBinary[Message[Msg]](data.unsafeArray)

  override def serializeNode(node: Msg): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeBinary(node))

  override def deserializeNode(data: ArraySeq.ofByte): Msg =
    custom.readBinary[Msg](data.unsafeArray)

  override def format(message: Message[Msg]): String =
    custom.write(message, indent)
}

case object UpickleMessagePackFormat {
  /** Message node type. */
  type Node = Msg
}
