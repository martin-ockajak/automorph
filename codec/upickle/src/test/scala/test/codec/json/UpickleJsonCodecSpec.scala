package test.codec.json

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec
import org.scalacheck.{Arbitrary, Gen}
import test.Generators.arbitraryRecord
import test.codec.CodecSpec
import test.{Enum, Record, Structure}
import ujson.{Bool, Num, Obj, Str, Value}

class UpickleJsonCodecSpec extends CodecSpec {

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
    "Encode / Decode" in {
      val custom = codec.custom
      implicit val recordRw: custom.ReadWriter[Record] = custom.macroRW
      Seq(recordRw)
      check { (record: Record) =>
        val encodedValue = codec.encode(record)
        val decodedValue = codec.decode[Record](encodedValue)
        decodedValue.equals(record)
      }
    }
  }
}

object UpickleJsonCodecSpec extends UpickleCustom {

  implicit def enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit def structureRw: ReadWriter[Structure] = macroRW
}
