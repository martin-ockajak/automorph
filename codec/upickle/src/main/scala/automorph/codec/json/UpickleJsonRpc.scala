package automorph.codec.json

import automorph.protocol.jsonrpc.{Message, MessageError}
import ujson.{Arr, Null, Num, Obj, Str, Value}
import upickle.core.Abort

/** JSON-RPC protocol support for uPickle message codec using JSON format. */
private[automorph] object UpickleJsonRpc {

  private[automorph] type RpcMessage = Message[Value]

  // Workaround for upickle bug causing the following error when using its
  // macroRW method to generate readers and writers for case classes with type parameters:
  //   java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
  //    at upickle.implicits.CaseClassReaderPiece$$anon$1.visitEnd(CaseClassReader.scala:30)
  //    at ujson.ByteParser.liftedTree1$1(ByteParser.scala:496)
  //    at ujson.ByteParser.tryCloseCollection(ByteParser.scala:496)
  //    at ujson.ByteParser.parseNested(ByteParser.scala:462)
  //    at ujson.ByteParser.parseTopLevel0(ByteParser.scala:323)
  final private[automorph] case class UpickleMessage(
    jsonrpc: Option[String],
    id: Option[Either[BigDecimal, String]],
    method: Option[String],
    params: Option[Either[List[Value], Map[String, Value]]],
    result: Option[Value],
    error: Option[UpickleMessageError]
  ) {

    def toProtocol: Message[Value] = Message[Value](
      jsonrpc,
      id,
      method,
      params,
      result,
      error.map(_.toProtocol)
    )
  }

  private[automorph] object UpickleMessage {

    def fromProtocol(v: Message[Value]): UpickleMessage = UpickleMessage(
      v.jsonrpc,
      v.id,
      v.method,
      v.params,
      v.result,
      v.error.map(UpickleMessageError.fromProtocol)
    )
  }

  final private[automorph] case class UpickleMessageError(
    message: Option[String],
    code: Option[Int],
    data: Option[Value]
  ) {

    def toProtocol: MessageError[Value] = MessageError[Value](
      message,
      code,
      data
    )
  }

  private[automorph] object UpickleMessageError {

    def fromProtocol(v: MessageError[Value]): UpickleMessageError = UpickleMessageError(
      v.message,
      v.code,
      v.data
    )
  }

  def readWriter[Custom <: UpickleJsonCustom](custom: Custom): custom.ReadWriter[Message[Value]] = {
    import custom._

    implicit val idRw: ReadWriter[Option[Message.Id]] = readwriter[Value].bimap[Option[Message.Id]](
      {
        case Some(Right(id)) => Str(id)
        case Some(Left(id)) => Num(id.toDouble)
        case None => Null
      },
      {
        case Str(id) => Some(Right(id))
        case Num(id) => Some(Left(BigDecimal(id)))
        case Null => None
        case id => throw Abort(s"Invalid request identifier: $id")
      }
    )
    implicit val paramsRw: ReadWriter[Option[Message.Params[Value]]] =
      readwriter[Value].bimap[Option[Message.Params[Value]]](
        {
          case Some(Right(params)) => Obj.from(params)
          case Some(Left(params)) => Arr(params)
          case None => Null
        },
        {
          case Obj(params) => Some(Right(params.toMap))
          case Arr(params) => Some(Left(params.toList))
          case Null => None
          case params => throw Abort(s"Invalid request parameters: $params")
        }
      )
    implicit val messageErrorRw: custom.ReadWriter[UpickleMessageError] = custom.macroRW
    implicit val customMessageRw: custom.ReadWriter[UpickleMessage] = custom.macroRW

    readwriter[UpickleMessage].bimap[Message[Value]](
      UpickleMessage.fromProtocol,
      _.toProtocol
    )
  }
}
