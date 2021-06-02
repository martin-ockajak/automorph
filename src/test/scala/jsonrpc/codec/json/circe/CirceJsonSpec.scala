package jsonrpc.codec.json.circe

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, parser}
import jsonrpc.codec.CodecSpec
import jsonrpc.codec.json.circe.CirceJsonCodec
import jsonrpc.spi.Codec
import jsonrpc.spi.Message.Params
import jsonrpc.util.ValueOps.asRight
import jsonrpc.{Enum, Record, Structure}

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

object JsonPickler extends CirceCodecs:

  given CirceEncoder[Enum] = Encoder.encodeInt.contramap[Enum](_.ordinal).wrap
  given CirceDecoder[Enum] = Decoder.decodeInt.map(Enum.fromOrdinal).wrap
  given CirceEncoder[Structure] = deriveEncoder[Structure].wrap
  given CirceDecoder[Structure] = deriveDecoder[Structure].wrap
  given CirceEncoder[Record] = deriveEncoder[Record].wrap
  given CirceDecoder[Record] = deriveDecoder[Record].wrap
