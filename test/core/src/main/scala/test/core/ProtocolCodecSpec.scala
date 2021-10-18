package test.core

import argonaut.Argonaut.jNumber
import argonaut.{Argonaut, CodecJson}
import automorph.codec.json.{ArgonautJsonCodec, CirceJsonCodec, JacksonJsonCodec, UpickleJsonCodec, UpickleJsonCustom}
import automorph.codec.messagepack.{UpickleMessagePackCodec, UpickleMessagePackCustom}
import automorph.protocol.JsonRpcProtocol
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.local.client.HandlerTransport
import automorph.{Client, Handler}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, SerializerProvider}
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import test.core.CoreSpec
import test.{Enum, Record, Structure}

trait ProtocolCodecSpec extends CoreSpec {

  private lazy val testFixtures = {
    implicit val usingContext: Context = contextValue
    Seq(
      {
        implicit lazy val enumEncoder: Encoder[Enum.Enum] = Encoder.encodeInt.contramap[Enum.Enum](Enum.toOrdinal)
        implicit lazy val enumDecoder: Decoder[Enum.Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
        implicit lazy val recordEncoder: Encoder[Record] = deriveEncoder[Record]
        implicit lazy val recordDecoder: Decoder[Record] = deriveDecoder[Record]
        val port = availablePort
        val codec = CirceJsonCodec()
        val protocol = JsonRpcProtocol[CirceJsonCodec.Node, codec.type](codec)
        val handler = Handler.protocol(protocol).system(system).context[Context]
          .bind(simpleApi).bind(complexApi)
        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
        val client = Client.protocol(protocol).transport(transport)
        TestFixture(
          client,
          handler,
          port,
          client.bind[SimpleApiType],
          client.bind[ComplexApiType],
          client.bind[InvalidApiType],
          (function, a0) => client.function(function).args(a0).call,
          (function, a0) => client.function(function).args(a0).tell
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
        val port = availablePort
        val codec = JacksonJsonCodec(JacksonJsonCodec.defaultMapper.registerModule(enumModule))
        val protocol = JsonRpcProtocol[JacksonJsonCodec.Node, codec.type](codec)
        val handler = Handler.protocol(protocol).system(system).context[Context]
          .bind(simpleApi).bind(complexApi)
        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
        val client = Client.protocol(protocol).transport(transport)
        TestFixture(
          client,
          handler,
          port,
          client.bind[SimpleApiType],
          client.bind[ComplexApiType],
          client.bind[InvalidApiType],
          (function, a0) => client.function(function).args(a0).call,
          (function, a0) => client.function(function).args(a0).tell
        )
      }, {
//        class Custom extends UpickleJsonCustom {
//          implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
//            value => Enum.toOrdinal(value),
//            number => Enum.fromOrdinal(number)
//          )
//          implicit lazy val structureRw: ReadWriter[Structure] = macroRW
//          implicit lazy val recordRw: ReadWriter[Record] = macroRW
//        }
//        val custom = new Custom
//        val port = availablePort
//        val codec = UpickleJsonCodec(custom)
//        val protocol = JsonRpcProtocol[UpickleJsonCodec.Node, codec.type](codec)
//        val handler = Handler.protocol(protocol).system(system).context[Context]
//          .bind(simpleApi).bind(complexApi)
//        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
//        val client = Client.protocol(protocol).transport(transport)
//        TestFixture(
//          client,
//          handler,
//          port,
//          client.bind[SimpleApiType],
//          client.bind[ComplexApiType],
//          client.bind[InvalidApiType],
//          (function, a0) => client.function(function).args(a0).call,
//          (function, a0) => client.function(function).args(a0).tell
//        )
//      }, {
//        class Custom extends UpickleMessagePackCustom {
//          implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
//            value => Enum.toOrdinal(value),
//            number => Enum.fromOrdinal(number)
//          )
//          implicit lazy val structureRw: ReadWriter[Structure] = macroRW
//          implicit lazy val recordRw: ReadWriter[Record] = macroRW
//        }
//        val custom = new Custom
//        val port = availablePort
//        val codec = UpickleMessagePackCodec(custom)
//        val protocol = JsonRpcProtocol[UpickleMessagePackCodec.Node, codec.type](codec)
//        val handler = Handler.protocol(protocol).system(system).context[Context]
//         .bind(simpleApi).bind(complexApi)
//        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
//        val client = Client.protocol(protocol).transport(transport)
//        TestFixture(
//          client,
//          handler,
//          port,
//          client.bind[SimpleApiType],
//          client.bind[ComplexApiType],
//          client.bind[InvalidApiType],
//          (function, a0) => client.function(function).args(a0).call,
//          (function, a0) => client.function(function).args(a0).tell
//        )
//      }, {
        implicit lazy val enumCodecJson: CodecJson[Enum.Enum] = CodecJson(
          (v: Enum.Enum) => jNumber(Enum.toOrdinal(v)),
          cursor => cursor.focus.as[Int].map(Enum.fromOrdinal)
        )
        implicit lazy val structureCodecJson: CodecJson[Structure] =
          Argonaut.codec1(Structure.apply, (v: Structure) => v.value)("value")
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
        val port = availablePort
        val codec = ArgonautJsonCodec()
        val protocol = JsonRpcProtocol[ArgonautJsonCodec.Node, codec.type](codec)
        val handler = Handler.protocol(protocol).system(system).context[Context]
          .bind(simpleApi).bind(complexApi)
        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
        val client = Client.protocol(protocol).transport(transport)
        TestFixture(
          client,
          handler,
          port,
          client.bind[SimpleApiType],
          client.bind[ComplexApiType],
          client.bind[InvalidApiType],
          (function, a0) => client.function(function).args(a0).call,
          (function, a0) => client.function(function).args(a0).tell
        )
      }
    )
  }

  override def fixtures: Seq[TestFixture] = testFixtures

  def customTransport(index: Int): Option[ClientMessageTransport[Effect, Context]] = None

  private def contextValue: Context = arbitraryContext.arbitrary.sample.get
}
