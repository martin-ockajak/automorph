package test.format.json

import argonaut.Argonaut.{jArray, jBool, jNumber, jString, jObjectAssocList}
import argonaut.{Argonaut, CodecJson, Json}
import automorph.format.json.ArgonautJsonFormat
import org.scalacheck.{Arbitrary, Gen}
import test.Generators.arbitraryRecord
import test.format.MessageFormatSpec
import test.{Enum, Record, Structure}

class ArgonautJsonSpec extends MessageFormatSpec {

  type Node = Json
  type ActualFormat = ArgonautJsonFormat

  override def format: ActualFormat = ArgonautJsonFormat()

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
    "Encode / Decode" in {
      check { (record: Record) =>
        val encodedValue = format.encode(record)
        val decodedValue = format.decode[Record](encodedValue)
        decodedValue.equals(record)
      }
    }
  }
}