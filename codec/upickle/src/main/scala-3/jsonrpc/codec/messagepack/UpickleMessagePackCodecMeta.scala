package jsonrpc.codec.messagepack

import jsonrpc.codec.json.UpickleJsonCodec.{Message, MessageError}
import jsonrpc.spi.Codec
import scala.compiletime.summonInline
import upack.Msg
import upickle.Api

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Customized custom Upickle reader and writer implicits instance type
 */
trait UpickleMessagePackCodecMeta[Customized <: Api] extends Codec[Msg]:
  this: UpickleMessagePackCodec[Customized] =>

  private val indent = 2
  private given customized.ReadWriter[Message] = customized.macroRW
  private given customized.ReadWriter[MessageError] = customized.macroRW

  override def mediaType: String = "application/json"

  override inline def encode[T](value: T): Msg =
    val writer = summonInline[customized.Writer[T]]
    customized.writeMsg(value)(using writer)

  override inline def decode[T](node: Msg): T =
    val reader = summonInline[customized.Reader[T]]
    customized.readBinary[T](node)(using reader)
