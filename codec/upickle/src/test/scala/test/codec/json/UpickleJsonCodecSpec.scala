package test.codec.json

import automorph.codec.common.UpickleCustom
import automorph.codec.json.UpickleJsonCodec
import org.scalacheck.{Arbitrary, Gen}
import test.Generators.arbitraryRecord
import test.codec.CodecSpec
import test.{Enum, Record, Structure}
import ujson.{Arr, Bool, Num, Obj, Str, Value}

class UpickleJsonCodecSpec extends CodecSpec {

  type Node = Value
  type ActualCodec = UpickleJsonCodec[UpickleJsonCodecSpec.type]

  override def codec: ActualCodec = UpickleJsonCodec(UpickleJsonCodecSpec)

  override lazy val arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.recursive[Node](recurse =>
    Gen.oneOf(
      Gen.resultOf(Str(_)),
      Gen.resultOf(Num(_)),
      Gen.resultOf(Bool(_)),
      Gen.listOfN[Node](2, recurse).map(Arr(_: _*)),
      Gen.mapOfN(2, Gen.zip(Arbitrary.arbitrary[String], recurse)).map(Obj.from)
    )
  ))

  private lazy val custom = codec.custom
  implicit private lazy val recordRw: custom.ReadWriter[Record] = custom.macroRW

  "" - {
    "TEST" in {
      val encode = (x: List[String]) => codec.encode(x)
    }
    "Encode / Decode" in {
      check { (record: Record) =>
        val encodedValue = codec.encode(record)
        val decodedValue = codec.decode[Record](encodedValue)
        decodedValue.equals(record)
      }
    }
  }
}

object UpickleJsonCodecSpec extends UpickleCustom {

  implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit lazy val structureRw: ReadWriter[Structure] = macroRW
}
