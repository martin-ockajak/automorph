package jsonrpc.json.upickle

import jsonrpc.core.Reflection
import jsonrpc.spi.{CallError, JsonContext, Message}
import jsonrpc.spi
import ujson.Value
import ujson.Str
import upickle.default.{Writer, Reader, ReadWriter, macroRW}
import upickle.{Api, AttributeTagged}
import scala.collection.immutable.ArraySeq
import scala.quoted.{Expr, Quotes, Type, quotes}

final case class UpickleJsonContext()
  extends JsonContext[Value] with AttributeTagged:
  type Json = Value

  private val indent = 2
  private given ReadWriter[UpickleJsonContext.Message] = macroRW
  private given ReadWriter[UpickleJsonContext.CallError] = macroRW

  def serialize(message: Message[Json]): ArraySeq.ofByte =
    // TODO: delete me
    //       semantics of ArraySeq.ofByte looked somewhat unclear in API
    //       inspection of stdlib sources suggests this does not copy the array
    //       test in REPL confermed the array is NOT copied, but unsafely wrapped
    //       (which is what we want for performance)
    ArraySeq.ofByte(writeToByteArray(UpickleJsonContext.Message(message)))

  def derialize(json: ArraySeq.ofByte): Message[Json] =
    read[UpickleJsonContext.Message](json.unsafeArray).toSpi

  def format(message: Message[Json]): String =
    write(UpickleJsonContext.Message(message), indent)

  def encode[T](value: T): Json = ???
//    writeJs[T](value)

  def decode[T](json: Json): T = ???
//    read[T](json)(reader[T])

  inline def yencode[T: Writer](value: T): Json =
    this.writeJs[T](value)

  def xencode[T](value: T): Value = ???

object UpickleJsonContext:
  type Json = Value

  inline def xencode[T](inline api: AttributeTagged, inline value: T): Value = ${xencode[T]('api, 'value)}

  private def xencode[T: Type](api: Expr[AttributeTagged], value: Expr[T])(using quotes: Quotes): Expr[Value] =
    val ref = Reflection(quotes)
    val apiTypeTree = ref.ast.TypeTree.of[AttributeTagged]
    val apiMethods = ref.publicApiMethods(apiTypeTree, concrete = false)
    val apiDescription = apiMethods.map(method => s"${method.name}: ${method.resultType.show}\n").mkString("\n")

//    apiMethods.filter(_.resultType.show.contains("Writer")).foreach { method =>
//      println(s"${method.name}: ${method.resultType.show}")
//    }
//    val publicMethods = introspection.publicMethods(introspection.ref.TypeTree.of[AttributeTagged])
//    val publicDescription = publicMethods.map(method => s"${method.name} - ${method.flags}\n").mkString("\n")
//    println(publicDescription)

    val valueType = ref.ast.TypeTree.of[T]
//    val call = ref.call(ref.term(api), "writeJs", List(valueType), List(List(ref.term(value))))
//    println(call)
    '{
//      ${call.asExpr}
//      ${api}.writeJs[String]("test")(using ${api}.StringWriter)
//      ${api}.writeJs[T](${value})
      Str("test")
    }

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

end UpickleJsonContext