package jsonrpc.codec.json

import io.circe.generic.auto
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, parser}
import jsonrpc.codec.CodecSpec
import jsonrpc.codec.json.JsonPickler
import jsonrpc.spi.Codec
import jsonrpc.spi.Message.Params
import jsonrpc.util.ValueOps.asRight
import jsonrpc.{Enum, Record, Structure}
import scala.language.implicitConversions

class CirceJsonSpec extends CodecSpec:

  type Node = Json
  type CodecType = CirceJsonCodec[JsonPickler.type]

  def codec: CodecType = CirceJsonCodec(JsonPickler)

  def messageArguments: Seq[Params[Node]] = Seq(
    Map(
      "x" -> Json.fromString("foo"),
      "y" -> Json.fromInt(1),
      "z" -> Json.fromBoolean(true)
    ).asRight
  )

  def messageResults: Seq[Json] = Seq(
    Json.fromString("test")
  )

  "" - {
    "Encode / Decode" in {
      val encodedJson = codec.encode(record)
      val decodedJson = codec.decode[Record](encodedJson)
      decodedJson.should(equal(record))
    }
  }

object JsonPickler extends CirceCustomized:

  given CirceEncoder[Enum] = Encoder.encodeInt.contramap[Enum](_.ordinal)
  given CirceDecoder[Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
  given CirceEncoder[Structure] = deriveEncoder[Structure]
  given CirceDecoder[Structure] = deriveDecoder[Structure]
  given CirceEncoder[Record] = deriveEncoder[Record]
  given CirceDecoder[Record] = deriveDecoder[Record]
