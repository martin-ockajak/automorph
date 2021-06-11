//package jsonrpc.codec.json
//
//import io.circe.*
//import io.circe.generic.auto
//import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
//import io.circe.syntax.EncoderOps
//import io.circe.{Decoder, Encoder, Json, parser}
//import jsonrpc.transport.local.HandlerTransport
//import jsonrpc.{ClientHandlerSpec, ComplexApi, Enum, Handler, Record, Structure}
//import scala.language.implicitConversions
//
//trait CirceJsonSpec extends ClientHandlerSpec:
//
//  type Node = Json
//  type CodecType = CirceJsonCodec[CirceJsonSpec.type]
//
//  override def codec: CodecType = CirceJsonCodec(CirceJsonSpec)
//
//  lazy val handler: Handler[Node, CodecType, Effect, Short] = Handler[Node, CodecType, Effect, Short](codec, backend)
//    .bind(simpleApiInstance).bind[ComplexApi[Effect]](complexApiInstance)
//
//  lazy val handlerTransport: HandlerTransport[Node, CodecType, Effect, Short] = HandlerTransport(handler, backend, 0)
//
//object CirceJsonSpec extends CirceCustomized:
//
//  given CirceEncoder[Enum] = Encoder.encodeInt.contramap[Enum](_.ordinal)
//  given CirceDecoder[Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
//  given CirceEncoder[Structure] = deriveEncoder[Structure]
//  given CirceDecoder[Structure] = deriveDecoder[Structure]
//  given CirceEncoder[Record] = deriveEncoder[Record]
//  given CirceDecoder[Record] = deriveDecoder[Record]
