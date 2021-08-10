package automorph.codec.messagepack

import automorph.codec.UpickleCustom
import automorph.protocol.jsonrpc.{Message, MessageError}
import scala.collection.mutable
import upack.{Arr, Float64, Msg, Null, Obj, Str}
import upickle.core.Abort

/**
 * JSON-RPC protocol support for uPickle message codec plugin.
 */
object JsonRpc {
  def readWriter[Custom <: UpickleCustom](custom: Custom): custom.ReadWriter[Message[Msg]] = {
    import custom._

    implicit val idRw: ReadWriter[Option[Message.Id]] = readwriter[Msg].bimap[Option[Message.Id]](
      {
        case Some(Right(id)) => Str(id)
        case Some(Left(id)) => Float64(id.toDouble)
        case None => Null
      },
      {
        case Str(id) => Some(Right(id))
        case Float64(id) => Some(Left(BigDecimal(id)))
        case Null => None
        case id => throw Abort(s"Invalid request identifier: $id")
      }
    )
    implicit val paramsRw: ReadWriter[Option[Message.Params[Msg]]] = readwriter[Msg].bimap[Option[Message.Params[Msg]]](
      {
        case Some(Right(params)) => Obj(mutable.LinkedHashMap[Msg, Msg](params.map { case (key, value) =>
          Str(key) -> value
        }.toSeq: _*))
        case Some(Left(params)) => Arr(params: _*)
        case None => Null
      },
      {
        case Obj(params) => Some(Right(params.toMap.map {
          case (Str(key), value) => key -> value
          case _ => throw Abort(s"Invalid request parameters: $params")
        }))
        case Arr(params) => Some(Left(params.toList))
        case Null => None
        case params => throw Abort(s"Invalid request parameters: $params")
      }
    )
    implicit val messageErrorRw: custom.ReadWriter[UpickleMessageError] = custom.macroRW
    implicit val customMessageRw: custom.ReadWriter[UpickleMessage] = custom.macroRW

    Seq(idRw, paramsRw, messageErrorRw, customMessageRw)
    readwriter[UpickleMessage].bimap[Message[Msg]](
      UpickleMessage.fromProtocol,
      _.toProtocol
    )
  }
}
