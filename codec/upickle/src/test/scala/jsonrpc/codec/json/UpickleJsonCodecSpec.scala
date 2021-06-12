package jsonrpc.codec.json

import jsonrpc.Enum.Enum
import jsonrpc.Generators.arbitraryRecord
import jsonrpc.codec.CodecSpec
import jsonrpc.codec.common.upickle.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.{Record, Structure}
import org.scalacheck.{Arbitrary, Gen}
import ujson.{Bool, Num, Obj, Str, Value}
import upickle.AttributeTagged

class UpickleJsonCodecSpec extends CodecSpec:

  type Node = Value
  type CodecType = UpickleJsonCodec[UpickleJsonCodecSpec.type]

  override def codec: CodecType = UpickleJsonCodec(UpickleJsonCodecSpec)

  override def arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.oneOf(Seq(
    Str("test"),
    Obj(
      "x" -> Str("foo"),
      "y" -> Num(1),
      "z" -> Bool(true)
    )
  )))

  "" - {
    "test" in {
      println(Custom.longFromNull())
      println(Custom.read[Long](ujson.Null))
//      codec.custom.read[Long](ujson.Null)(using reader)
    }
    "Encode / Decode" in {
      check { (record: Record) =>
        val encodedValue = codec.encode(record)
        val decodedValue = codec.decode[Record](encodedValue)
        decodedValue.equals(record)
      }
    }
  }

object Custom extends AttributeTagged:
  def longFromNull(): Long = read[Long](ujson.Null)

object UpickleJsonCodecSpec extends UpickleCustom:

  given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  given ReadWriter[Structure] = macroRW
  given ReadWriter[Record] = macroRW
