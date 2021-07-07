package test

import argonaut.Argonaut.jNumber
import argonaut.{Argonaut, CodecJson}
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}
import automorph.codec.common.UpickleCustom
import automorph.codec.json.{ArgonautJsonCodec, CirceJsonCodec, UpickleJsonCodec}
import automorph.codec.messagepack.UpickleMessagePackCodec
import automorph.spi.{Backend, Codec, Transport}
import automorph.transport.local.HandlerTransport
import automorph.{Client, Handler}
import scala.util.Try
import test.{ComplexApi, ComplexApiImpl, InvalidApi, InvalidApiImpl, SimpleApi, SimpleApiImpl}
import ujson.Value
import upack.Msg

trait CodecClientHandlerSpec extends ClientHandlerSpec {

  def codecFixtures: Seq[CodecFixture] = Seq(
    {
      val codec = UpickleJsonCodec(CodecClientHandlerSpec)
      val handler = Handler[Value, UpickleJsonCodec[CodecClientHandlerSpec.type], Effect, Context](codec, backend)
        .bind(simpleApiInstance).bind[ComplexApiType](complexApiInstance)
      val transport =
        customTransport.getOrElse(HandlerTransport(handler, backend, arbitraryContext.arbitrary.sample.get))
      val client = Client(codec, backend, transport)
      val clients = Seq(client, client.positional)
      CodecFixture(
        codec.getClass,
        client,
        handler,
        clients.map(_.bind[SimpleApiType]),
        clients.map(_.bind[ComplexApiType]),
        clients.map(_.bind[InvalidApiType]),
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).args(p1).call
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).namedArgs(p1).call
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).args(p1).tell
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).namedArgs(p1).tell
        }
      )
    }, {
      val codec = UpickleMessagePackCodec(CodecClientHandlerSpec)
      val handler = Handler[Msg, UpickleMessagePackCodec[CodecClientHandlerSpec.type], Effect, Context](codec, backend)
        .bind(simpleApiInstance).bind(complexApiInstance)
      val transport =
        customTransport.getOrElse(HandlerTransport(handler, backend, arbitraryContext.arbitrary.sample.get))
      val client = Client(codec, backend, transport)
      val clients = Seq(client, client.positional)
      CodecFixture(
        codec.getClass,
        client,
        handler,
        clients.map(_.bind[SimpleApiType]),
        clients.map(_.bind[ComplexApiType]),
        clients.map(_.bind[InvalidApiType]),
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).args(p1).call
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).namedArgs(p1).call
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).args(p1).tell
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).namedArgs(p1).tell
        }
      )
    }, {
      implicit lazy val enumEncoder: Encoder[Enum.Enum] = Encoder.encodeInt.contramap[Enum.Enum](Enum.toOrdinal)
      implicit lazy val enumDecoder: Decoder[Enum.Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
      implicit lazy val structureEncoder: Encoder[Structure] = deriveEncoder[Structure]
      implicit lazy val structureDecoder: Decoder[Structure] = deriveDecoder[Structure]
      val codec = CirceJsonCodec()
      val handler = Handler[Json, CirceJsonCodec, Effect, Context](codec, backend)
        .bind(simpleApiInstance).bind[ComplexApiType](complexApiInstance)
      val transport =
        customTransport.getOrElse(HandlerTransport(handler, backend, arbitraryContext.arbitrary.sample.get))
      val client = Client(codec, backend, transport)
      val clients = Seq(client, client.positional)
      CodecFixture(
        codec.getClass,
        client,
        handler,
        clients.map(_.bind[SimpleApiType]),
        clients.map(_.bind[ComplexApiType]),
        clients.map(_.bind[InvalidApiType]),
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).args(p1).call
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).namedArgs(p1).call
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).args(p1).tell
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).namedArgs(p1).tell
        }
      )
    }, {
      import ArgonautJsonCodec.noneCodecJson
      implicit lazy val enumCodecJson: CodecJson[Enum.Enum] = CodecJson(
        (v: Enum.Enum) => jNumber(Enum.toOrdinal(v)),
        cursor => cursor.focus.as[Int].map(Enum.fromOrdinal)
      )
      implicit lazy val structureCodecJson: CodecJson[Structure] =
        Argonaut.codec1(Structure.apply, (v: Structure) => (v.value))("value")
      implicit lazy val recordCodecJson: CodecJson[Record] =
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
      val codec = ArgonautJsonCodec()
      val handler = Handler[argonaut.Json, ArgonautJsonCodec, Effect, Context](codec, backend)
        .bind(simpleApiInstance).bind[ComplexApiType](complexApiInstance)
      val transport =
        customTransport.getOrElse(HandlerTransport(handler, backend, arbitraryContext.arbitrary.sample.get))
      val client = Client(codec, backend, transport)
      val clients = Seq(client, client.positional)
      CodecFixture(
        codec.getClass,
        client,
        handler,
        clients.map(_.bind[SimpleApiType]),
        clients.map(_.bind[ComplexApiType]),
        clients.map(_.bind[InvalidApiType]),
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).args(p1).call
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).namedArgs(p1).call
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).args(p1).tell
        },
        (method, p1, context) => {
          implicit val usingContext = context
          client.method(method).namedArgs(p1).tell
        }
      )
    }
  )

  def customTransport: Option[Transport[Effect, Context]] = None
}

object CodecClientHandlerSpec extends UpickleCustom {

  implicit def enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit def structureRw: ReadWriter[Structure] = macroRW
  implicit def recordRw: ReadWriter[Record] = macroRW
}
