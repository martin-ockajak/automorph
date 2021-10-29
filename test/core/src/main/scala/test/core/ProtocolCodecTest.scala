package test.core

import argonaut.Argonaut.jNumber
import argonaut.{Argonaut, CodecJson}
import automorph.codec.json.{ArgonautJsonCodec, CirceJsonCodec, JacksonJsonCodec}
import scala.annotation.nowarn
import automorph.codec.json.{UpickleJsonCodec, UpickleJsonCustom}
import automorph.codec.messagepack.{UpickleMessagePackCodec, UpickleMessagePackCustom}
import automorph.protocol.JsonRpcProtocol
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.local.client.HandlerTransport
import automorph.{Client, Handler, Types}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, SerializerProvider}
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import test.core.CoreTest
import test.{Enum, Record, Structure}

trait ProtocolCodecTest extends CoreTest {

  @nowarn("msg=used")
  private lazy val testFixtures: Seq[TestFixture] = {
    implicit val usingContext: Context = contextValue
    Seq(
      {
        implicit val enumEncoder: Encoder[Enum.Enum] = Encoder.encodeInt.contramap[Enum.Enum](Enum.toOrdinal)
        implicit val enumDecoder: Decoder[Enum.Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
        val codec = CirceJsonCodec()
        val protocol = JsonRpcProtocol[CirceJsonCodec.Node, codec.type](codec)
        val handler = Handler.protocol(protocol).system(system).context[Context]
          .bind(simpleApi).bind(complexApi)
        val transport = customTransport(handler).getOrElse(HandlerTransport(handler, system, contextValue))
        val client = Client.protocol(protocol).transport(transport)
        TestFixture(
          client,
          handler,
          client.bind[SimpleApiType],
          client.bind[ComplexApiType],
          client.bind[InvalidApiType],
          (function, a0) => client.call[String](function).args(a0),
          (function, a0) => client.notify(function).args(a0)
        )
      }, {
        val enumModule = new SimpleModule().addSerializer(
          classOf[Enum.Enum],
          new StdSerializer[Enum.Enum](classOf[Enum.Enum]) {

            override def serialize(value: Enum.Enum, generator: JsonGenerator, provider: SerializerProvider): Unit =
              generator.writeNumber(Enum.toOrdinal(value))
          }
        ).addDeserializer(
          classOf[Enum.Enum],
          new StdDeserializer[Enum.Enum](classOf[Enum.Enum]) {

            override def deserialize(parser: JsonParser, context: DeserializationContext): Enum.Enum =
              Enum.fromOrdinal(parser.getIntValue)
          }
        )
        val codec = JacksonJsonCodec(JacksonJsonCodec.defaultMapper.registerModule(enumModule))
        val protocol = JsonRpcProtocol[JacksonJsonCodec.Node, codec.type](codec)
        val handler = Handler.protocol(protocol).system(system).context[Context]
          .bind(simpleApi).bind(complexApi)
        val transport = customTransport(handler).getOrElse(HandlerTransport(handler, system, contextValue))
        val client = Client.protocol(protocol).transport(transport)
        TestFixture(
          client,
          handler,
          client.bind[SimpleApiType],
          client.bind[ComplexApiType],
          client.bind[InvalidApiType],
          (function, a0) => client.call[String](function).args(a0),
          (function, a0) => client.notify(function).args(a0)
        )
      }, {
        class Custom extends UpickleJsonCustom {
          implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
            value => Enum.toOrdinal(value),
            number => Enum.fromOrdinal(number)
          )
          implicit lazy val structureRw: ReadWriter[Structure] = macroRW
          implicit lazy val recordRw: ReadWriter[Record] = macroRW
        }
        val custom = new Custom
        val codec = UpickleJsonCodec(custom)
        val protocol = JsonRpcProtocol[UpickleJsonCodec.Node, codec.type](codec)
        val handler = Handler.protocol(protocol).system(system).context[Context]
          .bind(simpleApi).bind(complexApi)
        val transport = customTransport(handler).getOrElse(HandlerTransport(handler, system, contextValue))
        val client = Client.protocol(protocol).transport(transport)
        TestFixture(
          client,
          handler,
          client.bind[SimpleApiType],
          client.bind[ComplexApiType],
          client.bind[InvalidApiType],
          (function, a0) => client.call[String](function).args(a0),
          (function, a0) => client.notify(function).args(a0)
        )
      }, {
        class Custom extends UpickleMessagePackCustom {
          implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
            value => Enum.toOrdinal(value),
            number => Enum.fromOrdinal(number)
          )
          implicit lazy val structureRw: ReadWriter[Structure] = macroRW
          implicit lazy val recordRw: ReadWriter[Record] = macroRW
        }
        val custom = new Custom
        val codec = UpickleMessagePackCodec(custom)
        val protocol = JsonRpcProtocol[UpickleMessagePackCodec.Node, codec.type](codec)
        val handler = Handler.protocol(protocol).system(system).context[Context]
          .bind(simpleApi).bind(complexApi)
        val transport = customTransport(handler).getOrElse(HandlerTransport(handler, system, contextValue))
        val client = Client.protocol(protocol).transport(transport)
        TestFixture(
          client,
          handler,
          client.bind[SimpleApiType],
          client.bind[ComplexApiType],
          client.bind[InvalidApiType],
          (function, a0) => client.call[String](function).args(a0),
          (function, a0) => client.notify(function).args(a0)
        )
      }, {
        implicit val enumCodecJson: CodecJson[Enum.Enum] = CodecJson(
          (v: Enum.Enum) => jNumber(Enum.toOrdinal(v)),
          cursor => cursor.focus.as[Int].map(Enum.fromOrdinal)
        )
        implicit val structureCodecJson: CodecJson[Structure] =
          Argonaut.codec1(Structure.apply, (v: Structure) => v.value)("value")
        implicit val recordCodecJson: CodecJson[Record] =
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
        val protocol = JsonRpcProtocol[ArgonautJsonCodec.Node, codec.type](codec)
        val handler = Handler.protocol(protocol).system(system).context[Context]
          .bind(simpleApi).bind(complexApi)
        val transport = customTransport(handler).getOrElse(HandlerTransport(handler, system, contextValue))
        val client = Client.protocol(protocol).transport(transport)
        TestFixture(
          client,
          handler,
          client.bind[SimpleApiType],
          client.bind[ComplexApiType],
          client.bind[InvalidApiType],
          (function, a0) => client.call[String](function).args(a0),
          (function, a0) => client.notify(function).args(a0)
        )
      }
    )
  }

  override def fixtures: Seq[TestFixture] =
    testFixtures

  @nowarn("msg=used")
  def customTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] =
    None

  private def contextValue: Context =
    arbitraryContext.arbitrary.sample.get
}
