package test.codec.json

import io.circe.generic.auto.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}
import automorph.codec.json.CirceJsonCodec
import org.scalacheck.{Arbitrary, Gen}
import test.Generators.arbitraryRecord
import test.{Enum, Record, Structure}

class CirceJsonTest extends JsonMessageCodecTest {

  type Node = Json
  type ActualCodec = CirceJsonCodec

  override lazy val codec: ActualCodec = CirceJsonCodec()

  override lazy val arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.recursive[Node](recurse =>
    Gen.oneOf(
      Gen.resultOf(Json.fromString _),
      Gen.resultOf(Json.fromDoubleOrString _),
      Gen.resultOf(Json.fromBoolean _),
      Gen.listOfN[Node](2, recurse).map(Json.fromValues),
      Gen.mapOfN(2, Gen.zip(Arbitrary.arbitrary[String], recurse)).map(Json.fromFields)
    )
  ))

  implicit private lazy val enumEncoder: Encoder[Enum.Enum] = Encoder.encodeInt.contramap[Enum.Enum](Enum.toOrdinal)
  implicit private lazy val enumDecoder: Decoder[Enum.Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
  implicit private lazy val structureEncoder: Encoder[Structure] = deriveEncoder[Structure]
  implicit private lazy val structureDecoder: Decoder[Structure] = deriveDecoder[Structure]

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
