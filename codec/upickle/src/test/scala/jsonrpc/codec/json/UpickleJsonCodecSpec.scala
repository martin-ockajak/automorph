package jsonrpc.codec.json

import jsonrpc.codec.CodecSpec
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.spi.Codec
import jsonrpc.spi.Message.Params
import jsonrpc.{Enum, Record, Structure}
import ujson.{Bool, Num, Str, Value}
import upickle.AttributeTagged

trait UpickleJsonSpec extends CodecSpec:

  type Node = Value
  type CodecType = UpickleJsonCodec[JsonPickler.type]

  def codec: CodecType = UpickleJsonCodec(JsonPickler)

  def messageArguments: Seq[Params[Node]] = Seq(
    Right(Map(
      "x" -> Str("foo"),
      "y" -> Num(1),
      "z" -> Bool(true)
    ))
  )

  def messageResults: Seq[Value] = Seq(
    Str("test")
  )

  "" - {
    "Encode / Decode" in {
      val encodedValue = codec.encode(record)
      val decodedValue = codec.decode[Record](encodedValue)
      decodedValue.should(equal(record))
    }
  }

object JsonPickler extends AttributeTagged:

  given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  given ReadWriter[Structure] = macroRW
  given ReadWriter[Record] = macroRW
