package jsonrpc.codec.messagepack

import jsonrpc.spi.{Message, MessageError}
import upack.Msg

// Workaround for upickle bug causing the following error when using its
// macroRW method to generate readers and writers for case classes with type parameters:
//   java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
//    at upickle.implicits.CaseClassReaderPiece$$anon$1.visitEnd(CaseClassReader.scala:30)
//    at ujson.ByteParser.liftedTree1$1(ByteParser.scala:496)
//    at ujson.ByteParser.tryCloseCollection(ByteParser.scala:496)
//    at ujson.ByteParser.parseNested(ByteParser.scala:462)
//    at ujson.ByteParser.parseTopLevel0(ByteParser.scala:323)
private[jsonrpc] final case class MessagePackMessage(
  jsonrpc: Option[String],
  id: Option[Either[BigDecimal, String]],
  method: Option[String],
  params: Option[Either[List[Msg], Map[String, Msg]]],
  result: Option[Msg],
  error: Option[MessagePackMessageError]
) {

  def toSpi: Message[Msg] = Message[Msg](
    jsonrpc,
    id,
    method,
    params,
    result,
    error.map(_.toSpi)
  )
}

private[jsonrpc] object MessagePackMessage {

  def fromSpi(v: Message[Msg]): MessagePackMessage = MessagePackMessage(
    v.jsonrpc,
    v.id,
    v.method,
    v.params,
    v.result,
    v.error.map(MessagePackMessageError.fromSpi)
  )
}

private[jsonrpc] final case class MessagePackMessageError(
  code: Option[Int],
  message: Option[String],
  data: Option[Msg]
) {

  def toSpi: MessageError[Msg] = MessageError[Msg](
    code,
    message,
    data
  )
}

private[jsonrpc] object MessagePackMessageError {

  def fromSpi(v: MessageError[Msg]): MessagePackMessageError = MessagePackMessageError(
    v.code,
    v.message,
    v.data
  )
}
