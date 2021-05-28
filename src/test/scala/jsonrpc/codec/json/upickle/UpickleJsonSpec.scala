package jsonrpc.codec.json.upickle

import jsonrpc.{Enum, Record, Structure}
import jsonrpc.codec.CodecSpec
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.spi.Codec
import jsonrpc.spi.Message.Params
import jsonrpc.util.ValueOps.{asRight, asSome}
import ujson.{Bool, Num, Str, Value}
import upickle.AttributeTagged

class UpickleJsonSpec extends CodecSpec[Value]:

  private lazy val specificCodec = UpickleJsonCodec(Parser)

  def codec: Codec[Value] = specificCodec

  def messageArguments: Seq[Params[Value]] = Seq(
    Map(
      "x" -> Str("foo"),
      "y" -> Num(1),
      "z" -> Bool(true)
    ).asRight
  )

  def messageResults: Seq[Value] = Seq(
    Str("test")
  )

  "" - {
    "Encode / Decode" in {
      val encodedValue = specificCodec.encode(record)
      val decodedValue = specificCodec.decode[Record](encodedValue)
      decodedValue.should(equal(record))
    }
  }

object Parser extends AttributeTagged:

  given enumRw: ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  given structureRw: ReadWriter[Structure] = macroRW
  given recordRw: ReadWriter[Record] = macroRW
