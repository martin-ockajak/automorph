package test

import argonaut.Argonaut.{jNumber, jNull}
import argonaut.{Argonaut, DecodeResult, CodecJson}
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.{ArgonautJsonCodec, CirceJsonCodec, UpickleJsonCodec}
import jsonrpc.codec.messagepack.UpickleMessagePackCodec
import jsonrpc.spi.{Backend, Codec, Transport}
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.{Client, Handler}
import scala.util.Try
import test.{ComplexApi, ComplexApiImpl, InvalidApi, InvalidApiImpl, SimpleApi, SimpleApiImpl}
import ujson.Value
import upack.Msg

trait CodecClientHandlerSpec extends ClientHandlerSpec {

  lazy val codecFixtures: Seq[CodecFixture] = Seq(
    {
      val codec = UpickleJsonCodec(CodecClientHandlerSpec)
      val handler = Handler[Value, UpickleJsonCodec[CodecClientHandlerSpec.type], Effect, Context](codec, backend)
        .bind(simpleApiInstance).bind[ComplexApi[Effect, Context]](complexApiInstance)
      val transport = HandlerTransport(handler, backend, arbitraryContext.arbitrary.sample.get)
      val client = Client(codec, backend, transport)
      CodecFixture(
        codec.getClass,
        client,
        handler,
        Seq(client.bindByName[SimpleApi[Effect]], client.bindByPosition[SimpleApi[Effect]]),
        Seq(client.bindByName[ComplexApi[Effect, Context]], client.bindByPosition[ComplexApi[Effect, Context]]),
        Seq(client.bindByName[InvalidApi[Effect]], client.bindByPosition[InvalidApi[Effect]]),
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
      val codec = UpickleMessagePackCodec(CodecClientHandlerSpec)
      val handler = Handler[Msg, UpickleMessagePackCodec[CodecClientHandlerSpec.type], Effect, Context](codec, backend)
        .bind(simpleApiInstance).bind[ComplexApi[Effect, Context]](complexApiInstance)
      val transport = HandlerTransport(handler, backend, arbitraryContext.arbitrary.sample.get)
      val client = Client(codec, backend, transport)
      CodecFixture(
        codec.getClass,
        client,
        handler,
        Seq(client.bindByName[SimpleApi[Effect]], client.bindByPosition[SimpleApi[Effect]]),
        Seq(client.bindByName[ComplexApi[Effect, Context]], client.bindByPosition[ComplexApi[Effect, Context]]),
        Seq(client.bindByName[InvalidApi[Effect]], client.bindByPosition[InvalidApi[Effect]]),
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
      CodecFixture(
        codec.getClass,
        client,
        handler,
        Seq(client.bindByName[SimpleApi[Effect]], client.bindByPosition[SimpleApi[Effect]]),
        Seq(client.bindByName[ComplexApi[Effect, Context]], client.bindByPosition[ComplexApi[Effect, Context]]),
        Seq(client.bindByName[InvalidApi[Effect]], client.bindByPosition[InvalidApi[Effect]]),
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
      implicit lazy val noneCodecJson: CodecJson[None.type] = CodecJson(
        (v: None.type) => jNull,
        cursor => if (cursor.focus.isNull) DecodeResult.ok(None) else DecodeResult.fail("Not a null", cursor.history)
      )
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
        .bind(simpleApiInstance).bind[ComplexApi[Effect, Context]](complexApiInstance)
      val transport = HandlerTransport(handler, backend, arbitraryContext.arbitrary.sample.get)
      val client = Client(codec, backend, transport)
      CodecFixture(
        codec.getClass,
        client,
        handler,
        Seq(client.bindByName[SimpleApi[Effect]], client.bindByPosition[SimpleApi[Effect]]),
        Seq(client.bindByName[ComplexApi[Effect, Context]], client.bindByPosition[ComplexApi[Effect, Context]]),
        Seq(client.bindByName[InvalidApi[Effect]], client.bindByPosition[InvalidApi[Effect]]),
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
}

object CodecClientHandlerSpec extends UpickleCustom {

  implicit def enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit def structureRw: ReadWriter[Structure] = macroRW
  implicit def recordRw: ReadWriter[Record] = macroRW
}
