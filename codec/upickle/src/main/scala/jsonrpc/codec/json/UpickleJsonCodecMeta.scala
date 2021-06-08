package jsonrpc.codec.json

import jsonrpc.codec.json.UpickleJsonCodec.{Message, MessageError}
import jsonrpc.spi.Codec
import scala.compiletime.summonInline
import ujson.Value
import upickle.Api

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Customized customized Upickle reader and writer implicits instance type
 */
trait UpickleJsonCodecMeta[Customized <: Api] extends Codec[Value]:
  this: UpickleJsonCodec[Customized] =>

  private val indent = 2
  private given customized.ReadWriter[Message] = customized.macroRW
  private given customized.ReadWriter[MessageError] = customized.macroRW

  override def mediaType: String = "application/json"

  override inline def encode[T](value: T): Value =
    val writer = summonInline[customized.Writer[T]]
    customized.writeJs(value)(using writer)

  override inline def decode[T](node: Value): T =
    val reader = summonInline[customized.Reader[T]]
    customized.read[T](node)(using reader)
