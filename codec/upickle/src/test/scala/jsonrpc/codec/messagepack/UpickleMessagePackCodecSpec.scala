package jsonrpc.codec.messagepack

import jsonrpc.codec.CodecSpec
import jsonrpc.codec.messagepack.UpickleMessagePackCodec
import jsonrpc.spi.Codec
import jsonrpc.spi.Message.Params
import jsonrpc.{Enum, Generators, Record, Structure}
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
    given Arbitrary[Record] = Arbitrary(Arbitrary.arbitrary(Generators.arbitraryRecord).suchThat { record =>
      record.string.size < 100 &&
      record.structure.forall(_.value.size < 100) &&
      record.long < 1000 &&
      record.float.forall(_ < 1000.0) &&
      record.double < 1000.0
    })
    "Encode / Decode" in {
//       val record = Record(
//         "",
//         true,
//         43,
//         -1,
//         Some(516610680),
//         -9223372036854775808,
//         Some(-2.63055002E9.toFloat),
//         -1.9435757251505682E34,
//         Enum.Zero,
//         List(),
//         Map(),
//         Some(Structure("")),
//         Some("")
//       )
      val record: Record = Record(
        "test",
        boolean = true,
        0,
        1,
        Some(2),
        3,
        None,
        6.7,
        Enum.One,
        List("x", "y", "z"),
        Map(
          "foo" -> 0,
          "bar" -> 1
        ),
        Some(Structure(
          "test"
        )),
        None
      )
//      check { (record: Record) =>
        val encodedValue = codec.encode(record)
        val decodedValue = codec.decode[Record](encodedValue)
        decodedValue.equals(record)
//      }
    }
  }

object UpickleMessagePackCodecSpec extends AttributeTagged:

  given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  given ReadWriter[Structure] = macroRW
  given ReadWriter[Record] = macroRW
