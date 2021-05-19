package jsonrpc.format.json.upickle

import jsonrpc.core.Reflection
import jsonrpc.spi.{CallError, FormatContext, Message}
import jsonrpc.spi
import ujson.Value
import ujson.Str
import upickle.{Api, AttributeTagged}
import scala.collection.immutable.ArraySeq
import scala.quoted.{Expr, Quotes, Type, quotes}
import scala.compiletime.{erasedValue, error, summonInline}

final case class UpickleJsonFormat(parser: AttributeTagged)
  extends FormatContext[Value]:
  type Json = Value

  private val indent = 2
  private given parser.ReadWriter[UpickleJsonFormat.Message] = parser.macroRW
  private given parser.ReadWriter[UpickleJsonFormat.CallError] = parser.macroRW

  def serialize(message: Message[Json]): ArraySeq.ofByte =
    // TODO: delete me
    //       semantics of ArraySeq.ofByte looked somewhat unclear in API
    //       inspection of stdlib sources suggests this does not copy the array
    //       test in REPL confermed the array is NOT copied, but unsafely wrapped
    //       (which is what we want for performance)
    ArraySeq.ofByte(parser.writeToByteArray(UpickleJsonFormat.Message(message)))

  def derialize(json: ArraySeq.ofByte): Message[Json] =
    parser.read[UpickleJsonFormat.Message](json.unsafeArray).toSpi

  def format(message: Message[Json]): String =
    parser.write(UpickleJsonFormat.Message(message), indent)

//  def encode[T](value: T): Json = UpickleMacros.xencode(this, value)
  inline def encode[T](value: T): Json =
    val writer = summonInline[parser.Writer[T]]
    UpickleMacros.xencode(parser, writer, value)
  //    writeJs[T](value)

  inline def decode[T](json: Json): T =
    json.asInstanceOf[T]
//    read[T](json)(reader[T])

  def xencode[T](value: T): Value = ???

object UpickleJsonFormat:
  type Json = Value

  final case class Message(
    jsonrpc: Option[String],
    id: Option[Either[BigDecimal, String]],
    method: Option[String],
    params: Option[Either[List[Json], Map[String, Json]]],
    result: Option[Json],
    error: Option[CallError]
  ):
    def toSpi: spi.Message[Json] =
      spi.Message[Json](
        jsonrpc,
        id,
        method,
        params,
        result,
        error.map(_.toSpi)
      )

  object Message:

    def apply(v: spi.Message[Json]): Message =
      Message(
        v.jsonrpc,
        v.id,
        v.method,
        v.params,
        v.result,
        v.error.map(CallError.apply)
      )
  end Message

  final case class CallError(
    code: Option[Int],
    message: Option[String],
    data: Option[Json]
  ):
    def toSpi: spi.CallError[Json] =
      spi.CallError[Json](
        code,
        message,
        data
      )

  object CallError:

    def apply(v: spi.CallError[Json]): CallError = CallError(
      v.code,
      v.message,
      v.data
    )
  end CallError
