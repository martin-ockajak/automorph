package test.format.messagepack

import automorph.format.messagepack.UpickleMessagePackFormat
import automorph.format.UpickleCustom
import org.scalacheck.{Arbitrary, Gen}
import scala.annotation.nowarn
import scala.collection.mutable.LinkedHashMap
import test.Generators.arbitraryRecord
import test.format.FormatSpec
import test.{Enum, Record, Structure}
import upack.{Arr, Bool, Float64, Msg, Obj, Str}

class UpickleMessagePackSpec extends FormatSpec {

  type Node = Msg
  type ActualFormat = UpickleMessagePackFormat[UpickleMessagePackFormatSpec.type]

  override def format: ActualFormat = UpickleMessagePackFormat(UpickleMessagePackFormatSpec)

  override lazy val arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.recursive[Node](recurse =>
    Gen.oneOf(
      Gen.resultOf(Str(_)),
      Gen.resultOf(Float64(_)),
      Gen.resultOf(Bool(_)),
      Gen.listOfN[Node](2, recurse).map(Arr(_: _*)),
      Gen.mapOfN(2, Gen.zip(Gen.resultOf[String, Msg](Str(_)), recurse)).map(values => Obj(LinkedHashMap.from(values)))
    )
  ))

  private lazy val custom = format.custom
  implicit private lazy val recordRw: custom.ReadWriter[Record] = custom.macroRW
  Seq(recordRw)

  "" - {
    "Encode / Decode" in {
      check { (record: Record) =>
        val encodedValue = format.encode(record)
        val decodedValue = format.decode[Record](encodedValue)
        decodedValue.equals(record)
      }
    }
  }
}

object UpickleMessagePackFormatSpec extends UpickleCustom {

  implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )

  @nowarn
  implicit lazy val structureRw: ReadWriter[Structure] = macroRW
}
