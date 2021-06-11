package jsonrpc.codec.messagepack

import jsonrpc.codec.json.UpickleJsonCodec.{Message, MessageError}
import jsonrpc.spi.Codec
import scala.compiletime.summonInline
import upack.Msg
import upickle.Api

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom custom Upickle reader and writer implicits instance type
 */
trait UpickleMessagePackCodecMeta[Custom <: UpickleCustom] extends Codec[Msg]:
  this: UpickleMessagePackCodec[Custom] =>

  private val indent = 2
  private given custom.ReadWriter[Message] = custom.macroRW
  private given custom.ReadWriter[MessageError] = custom.macroRW

  override def mediaType: String = "application/json"

  override inline def encode[T](value: T): Msg =
    val writer = summonInline[custom.Writer[T]]
    custom.writeMsg(value)(using writer)

  override inline def decode[T](node: Msg): T =
    val reader = summonInline[custom.Reader[T]]
    custom.readBinary[T](node)(using reader)
