package test.codec.messagepack

import automorph.codec.messagepack.{UpickleMessagePackCustom, UpickleMessagePackCodec}
import org.scalacheck.{Arbitrary, Gen}
import scala.collection.mutable.LinkedHashMap
import test.Generators.arbitraryRecord
import test.codec.MessageCodecSpec
import test.{Enum, Record, Structure}
import upack.{Arr, Bool, Float64, Msg, Obj, Str}

class UpickleMessagePackSpec extends MessageCodecSpec {

  type Node = Msg
  type ActualCodec = UpickleMessagePackCodec[UpickleMessagePackSpec.type]

  override lazy val codec: ActualCodec = UpickleMessagePackCodec(UpickleMessagePackSpec)

  override lazy val arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.recursive[Node](recurse =>
    Gen.oneOf(
      Gen.resultOf(Str(_)),
      Gen.resultOf(Float64(_)),
      Gen.resultOf(Bool(_)),
      Gen.listOfN[Node](2, recurse).map(Arr(_: _*)),
      Gen.mapOfN(2, Gen.zip(Gen.resultOf[String, Msg](Str(_)), recurse)).map(values => Obj(LinkedHashMap.from(values)))
    )
  ))

  private lazy val custom = codec.custom
  implicit private lazy val recordRw: custom.ReadWriter[Record] = custom.macroRW
  Seq(recordRw)

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

object UpickleMessagePackSpec extends UpickleMessagePackCustom {

  implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )

  implicit lazy val structureRw: ReadWriter[Structure] = macroRW
}
