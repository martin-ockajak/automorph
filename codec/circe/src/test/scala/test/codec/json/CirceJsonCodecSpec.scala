package test.codec.json

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}
import jsonrpc.codec.json.{CirceCustom, CirceJsonCodec}
import org.scalacheck.{Arbitrary, Gen}
import test.Enum
import test.Generators.arbitraryRecord
import test.codec.CodecSpec
import test.{Record, Structure}

class CirceJsonSpec extends CodecSpec {

  type Node = Json
  type CodecType = CirceJsonCodec[CirceJsonCodecSpec.type]

  override def codec: CodecType = CirceJsonCodec(CirceJsonCodecSpec)

  override def arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.oneOf(Seq(
    Json.fromString("test"),
    Json.obj(
      "x" -> Json.fromString("foo"),
      "y" -> Json.fromInt(1),
      "z" -> Json.fromBoolean(true)
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
}

object CirceJsonCodecSpec extends CirceCustom {

  implicit val enumEncoder: CirceEncoder[Enum.Enum] = Encoder.encodeInt.contramap[Enum.Enum](Enum.toOrdinal)
  implicit val enumDecoder: CirceDecoder[Enum.Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
  implicit val structureEncoder: CirceEncoder[Structure] = deriveEncoder[Structure]
  implicit val structureDecoder: CirceDecoder[Structure] = deriveDecoder[Structure]
  implicit val recordEncoder: CirceEncoder[Record] = deriveEncoder[Record]
  implicit val recordDecoder: CirceDecoder[Record] = deriveDecoder[Record]
}
