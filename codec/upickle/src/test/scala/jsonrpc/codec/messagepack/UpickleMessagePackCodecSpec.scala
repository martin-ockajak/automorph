package jsonrpc.codec.messagepack

import jsonrpc.codec.CodecSpec
import jsonrpc.codec.messagepack.UpickleMessagePackCodec
import jsonrpc.{Enum, Record, Structure}
import jsonrpc.Generators.arbitraryRecord
import org.scalacheck.{Arbitrary, Gen}
import upack.{Bool, Float64, Msg, Obj, Str}
import upickle.AttributeTagged

class UpickleMessagePackSpec extends CodecSpec:

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
        val encodedValue = codec.encode(record)
        val decodedValue = codec.decode[Record](encodedValue)
        decodedValue.equals(record)
      }
    }
  }

object UpickleMessagePackCodecSpec extends AttributeTagged:

  given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  given ReadWriter[Structure] = macroRW
  given ReadWriter[Record] = macroRW
