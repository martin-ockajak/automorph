package test.format.json

import automorph.format.json.UpickleJsonFormat
import automorph.format.UpickleCustom
import org.scalacheck.{Arbitrary, Gen}
import test.Generators.arbitraryRecord
import test.format.FormatSpec
import test.{Enum, Record, Structure}
import ujson.{Arr, Bool, Num, Obj, Str, Value}

class UpickleJsonFormatSpec extends FormatSpec {

  type Node = Value
  type ActualFormat = UpickleJsonFormat[UpickleJsonFormatSpec.type]

  override def format: ActualFormat = UpickleJsonFormat(UpickleJsonFormatSpec)

  override lazy val arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.recursive[Node](recurse =>
    Gen.oneOf(
      Gen.resultOf(Str(_)),
      Gen.resultOf(Num(_)),
      Gen.resultOf(Bool(_)),
      Gen.listOfN[Node](2, recurse).map(Arr(_: _*)),
      Gen.mapOfN(2, Gen.zip(Arbitrary.arbitrary[String], recurse)).map(Obj.from)
    )
  ))

  private lazy val custom = format.custom
  implicit private lazy val recordRw: custom.ReadWriter[Record] = custom.macroRW

  "" - {
    "TEST" in {
      val encode = (x: List[String]) => format.encode(x)
    }
    "Encode / Decode" in {
      check { (record: Record) =>
        val encodedValue = format.encode(record)
        val decodedValue = format.decode[Record](encodedValue)
        decodedValue.equals(record)
      }
    }
  }
}

object UpickleJsonFormatSpec extends UpickleCustom {

  implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit lazy val structureRw: ReadWriter[Structure] = macroRW
}
