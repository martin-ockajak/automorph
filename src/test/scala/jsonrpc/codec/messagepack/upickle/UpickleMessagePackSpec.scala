package jsonrpc.codec.messagepack.upickle

import jsonrpc.codec.CodecSpec
import jsonrpc.codec.messagepack.upickle.UpickleMessagePackCodec
import jsonrpc.spi.Codec
import jsonrpc.spi.Message.Params
import jsonrpc.util.ValueOps.{asRight, asSome}
import jsonrpc.{Enum, Record, Structure}
import upack.{Bool, Float64, Msg, Str}
import upickle.AttributeTagged

class UpickleMessagePackSpec extends CodecSpec[Msg]:

  private lazy val specificCodec = UpickleMessagePackCodec(MessagePackParser)

  def codec: Codec[Msg] = specificCodec

  def messageArguments: Seq[Params[Msg]] = Seq(
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
//    "Encode / Decode" in {
//      val encodedValue = specificCodec.encode(record)
//      val decodedValue = specificCodec.decode[Record](encodedValue)
//      decodedValue.should(equal(record))
//    }
  }

object MessagePackParser extends AttributeTagged:

  given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  given ReadWriter[Structure] = macroRW
  given ReadWriter[Record] = macroRW
