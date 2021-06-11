package jsonrpc.codec.json

import jsonrpc.codec.CodecSpec
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.spi.Codec
import jsonrpc.spi.Message.Params
import jsonrpc.{Enum, Record, Structure}
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import ujson.{Bool, Num, Obj, Str, Value}
import upickle.AttributeTagged

trait UpickleJsonSpec extends CodecSpec:

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
      val encodedValue = codec.encode(record)
      val decodedValue = codec.decode[Record](encodedValue)
      decodedValue.should(equal(record))
    }
  }

object UpickleJsonCodecSpec extends AttributeTagged:

  given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  given ReadWriter[Structure] = macroRW
  given ReadWriter[Record] = macroRW
