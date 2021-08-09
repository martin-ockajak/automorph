package automorph.codec.json

import automorph.codec.{DefaultUpickleCustom, UpickleCustom}
import automorph.spi.Message
import scala.collection.immutable.ArraySeq
import ujson.{Arr, Num, Obj, Str, Value}
import upickle.core.Abort

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

  import custom._

  private val indent = 2

  implicit def idRw: ReadWriter[Message.Id] = readwriter[Value].bimap[Message.Id](
    {
      case Right(id) => Str(id)
      case Left(id) => Num(id.toDouble)
    },
    {
      case Str(id) => Right(id)
      case Num(id) => Left(BigDecimal(id))
      case id => throw Abort(s"Invalid request identifier: $id")
    }
  )

  implicit def paramsRw: ReadWriter[Message.Params[Value]] = readwriter[Value].bimap[Message.Params[Value]](
    {
      case Right(params) => Obj.from(params)
      case Left(params) => Arr(params)
    },
    {
      case Obj(params) => Right(params.toMap)
      case Arr(params) => Left(params.toList)
      case params => throw Abort(s"Invalid request parameters: $params")
    }
  )

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

  override def text(message: Message[Value]): String =
    custom.write(UpickleMessage.fromSpi(message), indent)
}

case object UpickleJsonCodec {
  /** Message node type. */
  type Node = Value
}
