package test.codec.json

import argonaut.Argonaut.{jArray, jBool, jNumber, jString, jObjectAssocList}
import argonaut.{Argonaut, CodecJson, Json}
import automorph.codec.json.ArgonautJsonCodec
import org.scalacheck.{Arbitrary, Gen}
import test.Generators.arbitraryRecord
import test.codec.json.JsonMessageCodecTest
import test.{Enum, Record, Structure}

class ArgonautJsonTest extends JsonMessageCodecTest {

  type Node = Json
  type ActualCodec = ArgonautJsonCodec

  override lazy val codec: ActualCodec = ArgonautJsonCodec()

  override lazy val arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.recursive[Node](recurse =>
    Gen.oneOf(
      Gen.resultOf(jString),
      Gen.resultOf((value: Int) => jNumber(value)),
      Gen.resultOf(jBool),
      Gen.listOfN[Node](2, recurse).map(jArray),
      Gen.mapOfN(2, Gen.zip(Arbitrary.arbitrary[String], recurse)).map(values => jObjectAssocList(values.toList))
    )
  ))

  implicit private lazy val enumCodecJson: CodecJson[Enum.Enum] = CodecJson(
    (v: Enum.Enum) => jNumber(Enum.toOrdinal(v)),
    cursor => cursor.focus.as[Int].map(Enum.fromOrdinal)
  )

  implicit private lazy val structureCodecJson: CodecJson[Structure] =
    Argonaut.codec1(Structure.apply, (v: Structure) => (v.value))("value")

  implicit private lazy val recordCodecJson: CodecJson[Record] =
    Argonaut.codec13(
      Record.apply,
      (v: Record) =>
        (
          v.string,
          v.boolean,
          v.byte,
          v.short,
          v.int,
          v.long,
          v.float,
          v.double,
          v.enumeration,
          v.list,
          v.map,
          v.structure,
          v.none
        )
    )(
      "string",
      "boolean",
      "byte",
      "short",
      "int",
      "long",
      "float",
      "double",
      "enumeration",
      "list",
      "map",
      "structure",
      "none"
    )

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
