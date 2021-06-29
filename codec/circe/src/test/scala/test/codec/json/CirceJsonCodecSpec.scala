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
      import CirceJsonCodecSpec._
      check { (record: Record) =>
        val encodedValue = codec.encode(record)
        val decodedValue = codec.decode[Record](encodedValue)
        decodedValue.equals(record)
      }
    }
  }
}

object CirceJsonCodecSpec extends CirceCustom {

  implicit def enumEncoder: Encoder[Enum.Enum] = Encoder.encodeInt.contramap[Enum.Enum](Enum.toOrdinal)
  implicit def enumDecoder: Decoder[Enum.Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
  implicit def structureEncoder: Encoder[Structure] = deriveEncoder[Structure]
  implicit def structureDecoder: Decoder[Structure] = deriveDecoder[Structure]
  implicit def recordEncoder: Encoder[Record] = deriveEncoder[Record]
  implicit def recordDecoder: Decoder[Record] = deriveDecoder[Record]
}
