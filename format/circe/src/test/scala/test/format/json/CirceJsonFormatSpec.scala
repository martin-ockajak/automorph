package test.format.json

import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}
import automorph.format.json.CirceJsonFormat
import org.scalacheck.{Arbitrary, Gen}
import test.Generators.arbitraryRecord
import test.format.FormatSpec
import test.{Enum, Record, Structure}

class CirceJsonFormatSpec extends FormatSpec {

  type Node = Json
  type ActualFormat = CirceJsonFormat

  override def format: ActualFormat = CirceJsonFormat()

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
    "Encode / Decode" in {
      check { (record: Record) =>
        val encodedValue = format.encode(record)
        val decodedValue = format.decode[Record](encodedValue)
        decodedValue.equals(record)
      }
    }
  }
}
