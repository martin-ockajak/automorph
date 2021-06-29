package test.codec.messagepack

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.messagepack.UpickleMessagePackCodec
import org.scalacheck.{Arbitrary, Gen}
import scala.annotation.nowarn
import test.Generators.arbitraryRecord
import test.codec.CodecSpec
import test.{Enum, Record, Structure}
import upack.{Bool, Float64, Msg, Obj, Str}

class UpickleMessagePackSpec extends CodecSpec {

  type Node = Msg
  type CodecType = UpickleMessagePackCodec[UpickleMessagePackCodecSpec.type]

  override def codec: CodecType = UpickleMessagePackCodec(UpickleMessagePackCodecSpec)

  override def arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.oneOf(Seq(
    Str("test"),
    Obj(
      Str("x") -> Str("foo"),
      Str("y") -> Float64(1),
      Str("z") -> Bool(true)
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

object UpickleMessagePackCodecSpec extends UpickleCustom {

  implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )

  @nowarn
  implicit lazy val structureRw: ReadWriter[Structure] = macroRW
}
