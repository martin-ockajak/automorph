package test

import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.{CirceJsonCodec, UpickleJsonCodec}
import jsonrpc.spi.{Backend, Codec, Transport}
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.{Client, Handler}
import scala.util.Try
import test.{ComplexApi, ComplexApiImpl, InvalidApi, InvalidApiImpl, SimpleApi, SimpleApiImpl}
import ujson.Value

trait CodecClientHandlerSpec extends ClientHandlerSpec {

  lazy val codecFixtures: Seq[CodecFixture] = Seq(
    {
      val codec = UpickleJsonCodec(CodecClientHandlerSpec)
      val handler = Handler[Value, UpickleJsonCodec[CodecClientHandlerSpec.type], Effect, Context](codec, backend)
        .bind(simpleApiInstance).bind[ComplexApi[Effect, Context]](complexApiInstance)
      val transport = HandlerTransport(handler, backend, arbitraryContext.arbitrary.sample.get)
      val client = Client(codec, backend, transport)
      val clients = makeClients(client)
      CodecFixture(
        codec.getClass,
        client,
        handler,
        clients.map(_.bind[SimpleApi[Effect]]),
        clients.map(_.bind[ComplexApi[Effect, Context]]),
        clients.map(_.bind[InvalidApi[Effect]]),
        (method, p1, context) => {
          implicit val usingContext = context
          client.callByPosition[String, String](method, p1)
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.callByName[String, String](method, p1)
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.notifyByPosition[String](method, p1)
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.notifyByName[String](method, p1)
        }
      )
    }, {
      implicit lazy val enumEncoder: Encoder[Enum.Enum] = Encoder.encodeInt.contramap[Enum.Enum](Enum.toOrdinal)
      implicit lazy val enumDecoder: Decoder[Enum.Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
      implicit lazy val structureEncoder: Encoder[Structure] = deriveEncoder[Structure]
      implicit lazy val structureDecoder: Decoder[Structure] = deriveDecoder[Structure]
      val codec = CirceJsonCodec()
      val handler = Handler[Json, CirceJsonCodec, Effect, Context](codec, backend)
        .bind(simpleApiInstance).bind[ComplexApi[Effect, Context]](complexApiInstance)
      val transport = HandlerTransport(handler, backend, arbitraryContext.arbitrary.sample.get)
      val client = Client(codec, backend, transport)
      val clients = makeClients(client)
      CodecFixture(
        codec.getClass,
        client,
        handler,
        clients.map(_.bind[SimpleApi[Effect]]),
        clients.map(_.bind[ComplexApi[Effect, Context]]),
        clients.map(_.bind[InvalidApi[Effect]]),
        (method, p1, context) => {
          implicit val usingContext = context
          client.callByPosition[String, String](method, p1)
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.callByName[String, String](method, p1)
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.notifyByPosition[String](method, p1)
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.notifyByName[String](method, p1)
        }
      )
    }
  )

  private def makeClients[Node, ExactCodec <: Codec[Node]](client: Client[Node, ExactCodec, Effect, Context])
    : Seq[Client[Node, ExactCodec, Effect, Context]] =
    Seq(client, client.copy(argumentsByName = false))
}

object CodecClientHandlerSpec extends UpickleCustom {

  implicit def enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit def structureRw: ReadWriter[Structure] = macroRW
  implicit def recordRw: ReadWriter[Record] = macroRW
}
