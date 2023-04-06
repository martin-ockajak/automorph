package test.codec.json

import automorph.codec.json.{UpickleJsonCodec, UpickleJsonCustom}
import org.scalacheck.{Arbitrary, Gen}
import test.Generators.arbitraryRecord
import test.codec.json.JsonMessageCodecTest
import test.{Enum, Record, Structure}
import ujson.{Arr, Bool, Null, Num, Obj, Str, Value}

class UpickleJsonTest extends JsonMessageCodecTest {

  type Node = Value
  type ActualCodec = UpickleJsonCodec[UpickleJsonTest.type]

  override lazy val codec: ActualCodec = UpickleJsonCodec(UpickleJsonTest)

  override lazy val arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.recursive[Node](recurse =>
    Gen.oneOf(
      Gen.const(Null),
      Gen.resultOf(Str(_)),
      Gen.resultOf(Num(_)),
      Gen.resultOf(Bool(_)),
      Gen.listOfN[Node](2, recurse).map(Arr(_: _*)),
      Gen.mapOfN(2, Gen.zip(Arbitrary.arbitrary[String], recurse)).map(Obj.from)
    )
  ))

  private lazy val custom = codec.custom
  private implicit lazy val recordRw: custom.ReadWriter[Record] = custom.macroRW

  "" - {
    "Encode & Decode" in {
      check { (record: Record) =>
        val encoded = codec.encode(record)
        val decoded = codec.decode[Record](encoded)
        decoded.equals(record)
      }
    }
  }
}

case object UpickleJsonTest extends UpickleJsonCustom {

  implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit lazy val structureRw: ReadWriter[Structure] = macroRW
}
