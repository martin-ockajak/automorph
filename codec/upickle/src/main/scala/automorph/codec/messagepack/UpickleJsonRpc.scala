package automorph.codec.messagepack

import automorph.codec.UpickleCustom
import automorph.protocol.jsonrpc.{Message, MessageError}
import scala.collection.mutable
import upack.{Arr, Float64, Msg, Null, Obj, Str}
import upickle.core.Abort

/**
 * JSON-RPC protocol support for uPickle message codec plugin.
 */
object UpickleJsonRpc {
// FIXME - restore
// private[automorph] case object UpickleJsonRpc {
  private[automorph] type Data = Message[Msg]

  // Workaround for upickle bug causing the following error when using its
  // macroRW method to generate readers and writers for case classes with type parameters:
  //   java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
  //    at upickle.implicits.CaseClassReaderPiece$$anon$1.visitEnd(CaseClassReader.scala:30)
  //    at ujson.ByteParser.liftedTree1$1(ByteParser.scala:496)
  //    at ujson.ByteParser.tryCloseCollection(ByteParser.scala:496)
  //    at ujson.ByteParser.parseNested(ByteParser.scala:462)
  //    at ujson.ByteParser.parseTopLevel0(ByteParser.scala:323)
  private[automorph] final case class UpickleMessage(
    jsonrpc: Option[String],
    id: Option[Either[BigDecimal, String]],
    method: Option[String],
    params: Option[Either[List[Msg], Map[String, Msg]]],
    result: Option[Msg],
    error: Option[UpickleMessageError]
  ) {

    def toProtocol: Message[Msg] = Message[Msg](
      jsonrpc,
      id,
      method,
      params,
      result,
      error.map(_.toProtocol)
    )
  }

  private[automorph] object UpickleMessage {

    def fromProtocol(v: Message[Msg]): UpickleMessage = UpickleMessage(
      v.jsonrpc,
      v.id,
      v.method,
      v.params,
      v.result,
      v.error.map(UpickleMessageError.fromProtocol)
    )
  }

  private[automorph] final case class UpickleMessageError(
    message: Option[String],
    code: Option[Int],
    data: Option[Msg]
  ) {

    def toProtocol: MessageError[Msg] = MessageError[Msg](
      message,
      code,
      data
    )
  }

  private[automorph] object UpickleMessageError {

    def fromProtocol(v: MessageError[Msg]): UpickleMessageError = UpickleMessageError(
      v.message,
      v.code,
      v.data
    )
  }

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
