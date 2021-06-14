package test.codec.messagepack

import jsonrpc.Enum.Enum
import jsonrpc.Generators.arbitraryRecord
import jsonrpc.codec.CodecSpec
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.messagepack.UpickleMessagePackCodec
import jsonrpc.{Record, Structure}
import org.scalacheck.{Arbitrary, Gen}
import upack.{Bool, Float64, Msg, Obj, Str}
import upickle.AttributeTagged

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
      check { (record: Record) =>
        try
          val encodedValue = codec.encode(record)
          val decodedValue = codec.decode[Record](encodedValue)
          decodedValue.equals(record)
        catch
          case error =>
            println(error)
            throw error
      }
    }
  }
}

object UpickleMessagePackCodecSpec extends UpickleCustom {

  implicit def enumRw: ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  implicit def structureRw: ReadWriter[Structure] = macroRW
  implicit def recordRw: ReadWriter[Record] = macroRW
}
