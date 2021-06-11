package jsonrpc.codec.json

import jsonrpc.codec.common.upickle.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec.{Message, MessageError}
import jsonrpc.spi.Codec
import scala.compiletime.summonInline
import ujson.Value

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
trait UpickleJsonCodecMeta[Custom <: UpickleCustom] extends Codec[Value]:
  this: UpickleJsonCodec[Custom] =>

  private val indent = 2
  private given custom.ReadWriter[Message] = custom.macroRW
  private given custom.ReadWriter[MessageError] = custom.macroRW

  override def mediaType: String = "application/json"

  inline def test[T](): T =
    val reader = compiletime.summonInline[custom.Reader[T]]
    custom.read[T](ujson.Null)(using reader)

  override inline def encode[T](value: T): Value =
    val writer = summonInline[custom.Writer[T]]
    custom.writeJs(value)(using writer)

  override inline def decode[T](node: Value): T =
    val reader = summonInline[custom.Reader[T]]
    custom.read[T](node)(using reader)
