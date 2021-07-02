package test.codec.json

import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}
import jsonrpc.Handler
import jsonrpc.codec.json.CirceJsonCodec
import jsonrpc.transport.local.HandlerTransport
import test.{ClientHandlerSpec, ComplexApi, Enum, InvalidApi, Record, SimpleApi, Structure}

trait CirceJsonSpec extends ClientHandlerSpec {

  type Node = Json
  type ExactCodec = CirceJsonCodec

  override def simpleApis: Seq[SimpleApi[Effect]] = clients.map(_.bind[SimpleApi[Effect]])

  override def complexApis: Seq[ComplexApi[Effect, Context]] = clients.map(_.bind[ComplexApi[Effect, Context]])

  override def invalidApis: Seq[InvalidApi[Effect]] = clients.map(_.bind[InvalidApi[Effect]])

  override def callByPosition(method: String, p1: String)(implicit context: Context): Effect[String] =
    client.callByPosition[String, String](method, "test")

  def handler: Handler[Node, ExactCodec, Effect, Context]

  implicit private lazy val enumEncoder: Encoder[Enum.Enum] = Encoder.encodeInt.contramap[Enum.Enum](Enum.toOrdinal)
  implicit private lazy val enumDecoder: Decoder[Enum.Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
  implicit private lazy val structureEncoder: Encoder[Structure] = deriveEncoder[Structure]
  implicit private lazy val structureDecoder: Decoder[Structure] = deriveDecoder[Structure]

  lazy val codec: ExactCodec = CirceJsonCodec()

  lazy val handlerTransport: HandlerTransport[Node, ExactCodec, Effect, Context] = {
    val boundHandler = handler.bind(simpleApiInstance).bind[ComplexApi[Effect, Context]](complexApiInstance)
    HandlerTransport(boundHandler, backend, arbitraryContext.arbitrary.sample.get)
  }
}
