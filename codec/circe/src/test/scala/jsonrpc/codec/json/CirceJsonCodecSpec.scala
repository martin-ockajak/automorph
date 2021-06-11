package jsonrpc.codec.json

import io.circe.generic.auto
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, parser}
import jsonrpc.codec.CodecSpec
import jsonrpc.codec.json.CirceJsonCodecSpec
import jsonrpc.spi.Codec
import jsonrpc.spi.Message.Params
import jsonrpc.{Enum, Record, Structure}
import org.scalacheck.{Arbitrary, Gen}
import scala.language.implicitConversions

class CirceJsonSpec extends CodecSpec:

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
      val encodedJson = codec.encode(record)
      val decodedJson = codec.decode[Record](encodedJson)
      decodedJson.should(equal(record))
    }
  }

object CirceJsonCodecSpec extends CirceCustomized:

  given CirceEncoder[Enum] = Encoder.encodeInt.contramap[Enum](_.ordinal)
  given CirceDecoder[Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
  given CirceEncoder[Structure] = deriveEncoder[Structure]
  given CirceDecoder[Structure] = deriveDecoder[Structure]
  given CirceEncoder[Record] = deriveEncoder[Record]
  given CirceDecoder[Record] = deriveDecoder[Record]
