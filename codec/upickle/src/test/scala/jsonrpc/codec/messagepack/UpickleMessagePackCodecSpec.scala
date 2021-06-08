package jsonrpc.codec.messagepack

import jsonrpc.codec.CodecSpec
import jsonrpc.codec.messagepack.UpickleMessagePackCodec
import jsonrpc.spi.Codec
import jsonrpc.spi.Message.Params
import jsonrpc.{Enum, Record, Structure}
import upack.{Bool, Float64, Msg, Str}
import upickle.AttributeTagged

class UpickleMessagePackSpec extends CodecSpec:

  type Node = Msg
  type CodecType = UpickleMessagePackCodec[MessagePackPickler.type]

  def codec: CodecType = UpickleMessagePackCodec(MessagePackPickler)

  def messageArguments: Seq[Params[Node]] = Seq(
    Right(Map(
      "x" -> Str("foo"),
      "y" -> Float64(1),
      "z" -> Bool(true)
    ))
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
