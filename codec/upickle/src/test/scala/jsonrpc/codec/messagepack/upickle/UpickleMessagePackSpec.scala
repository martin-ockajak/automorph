package jsonrpc.codec.messagepack.upickle

import jsonrpc.codec.CodecSpec
import jsonrpc.codec.messagepack.upickle.UpickleMessagePackCodec
import jsonrpc.spi.Codec
import jsonrpc.spi.Message.Params
import jsonrpc.util.ValueOps.asRight
import jsonrpc.{Enum, Record, Structure}
import upack.{Bool, Float64, Msg, Str}
import upickle.AttributeTagged

class UpickleMessagePackSpec extends CodecSpec:

  type Node = Msg
  type CodecType = UpickleMessagePackCodec[MessagePackPickler.type]

  def codec: CodecType = UpickleMessagePackCodec(MessagePackPickler)

  def messageArguments: Seq[Params[Node]] = Seq(
    Map(
      "x" -> Str("foo"),
      "y" -> Float64(1),
      "z" -> Bool(true)
    ).asRight
  )

  def messageResults: Seq[Msg] = Seq(
    Str("test")
  )

  "" - {
    "Encode / Decode" in {
      val encodedValue = codec.encode(record)
      val decodedValue = codec.decode[Record](encodedValue)
      decodedValue.should(equal(record))
    }
  }

object MessagePackPickler extends AttributeTagged:

  given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  given ReadWriter[Structure] = macroRW
  given ReadWriter[Record] = macroRW
