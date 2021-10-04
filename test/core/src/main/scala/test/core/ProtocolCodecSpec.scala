package test.core

import argonaut.Argonaut.jNumber
import argonaut.{Argonaut, CodecJson}
import automorph.codec.UpickleCustom
import automorph.codec.json.{ArgonautJsonCodec, CirceJsonCodec, JacksonJsonCodec, UpickleJsonCodec}
import automorph.codec.messagepack.UpickleMessagePackCodec
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
        val port = availablePort
        val codec = CirceJsonCodec()
        val protocol = JsonRpcProtocol(codec)
        val handler = Handler[CirceJsonCodec.Node, CirceJsonCodec, Effect, Context](codec, system, protocol)
          .bind(simpleApiInstance).bind(complexApiInstance)
        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
        val client = Client[CirceJsonCodec.Node, CirceJsonCodec, Effect, Context](codec, system, transport, protocol)
        TestFixture(
          codec.getClass,
          client,
          handler,
          port,
          Seq(client.bind[SimpleApiType], client.bindPositional[SimpleApiType]),
          Seq(client.bind[ComplexApiType], client.bindPositional[ComplexApiType]),
          Seq(client.bind[InvalidApiType], client.bindPositional[InvalidApiType]),
          (method, p1) => client.method(method).positional.args(p1).call,
          (method, p1) => client.method(method).args(p1).call,
          (method, p1) => client.method(method).positional.args(p1).tell,
          (method, p1) => client.method(method).args(p1).tell
        )
// FIXME - enable
//      }, {
//        val enumModule = new SimpleModule().addSerializer(
//          classOf[Enum.Enum],
//          new StdSerializer[Enum.Enum](classOf[Enum.Enum]) {
//
//            override def serialize(value: Enum.Enum, generator: JsonGenerator, provider: SerializerProvider): Unit =
//              generator.writeNumber(Enum.toOrdinal(value))
//          }
//        ).addDeserializer(
//          classOf[Enum.Enum],
//          new StdDeserializer[Enum.Enum](classOf[Enum.Enum]) {
//
//            override def deserialize(parser: JsonParser, context: DeserializationContext): Enum.Enum =
//              Enum.fromOrdinal(parser.getIntValue)
//          }
//        )
//        val port = availablePort
//        val codec = JacksonJsonCodec()
//        val protocol = JsonRpcProtocol(codec)
//        val handler = Handler[JacksonJsonCodec.Node, JacksonJsonCodec, Effect, Context](codec, system, protocol)
//          .bind(simpleApiInstance).bind(complexApiInstance)
//        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
//        val client = Client[JacksonJsonCodec.Node, JacksonJsonCodec, Effect, Context](codec, system, transport, protocol)
//        TestFixture(
//          codec.getClass,
//          client,
//          handler,
//          port,
//          Seq(client.bind[SimpleApiType], client.bindPositional[SimpleApiType]),
//          Seq(client.bind[ComplexApiType], client.bindPositional[ComplexApiType]),
//          Seq(client.bind[InvalidApiType], client.bindPositional[InvalidApiType]),
//          (method, p1) => client.method(method).positional.args(p1).call,
//          (method, p1) => client.method(method).args(p1).call,
//          (method, p1) => client.method(method).positional.args(p1).tell,
//          (method, p1) => client.method(method).args(p1).tell
//        )
//      }, {
//        val port = availablePort
//        val codec = UpickleJsonCodec(ProtocolCodecSpec)
//        val protocol = JsonRpcProtocol(codec)
//        val handler = Handler[UpickleJsonCodec.Node, UpickleJsonCodec[ProtocolCodecSpec.type], Effect, Context](
//          codec,
//          system,
//          protocol
//        )
//          .bind(simpleApiInstance).bind(complexApiInstance)
//        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
//        val client = Client[UpickleJsonCodec.Node, UpickleJsonCodec[ProtocolCodecSpec.type], Effect, Context](codec, system, transport, protocol)
//        TestFixture(
//          codec.getClass,
//          client,
//          handler,
//          port,
//          Seq(client.bind[SimpleApiType], client.bindPositional[SimpleApiType]),
//          Seq(client.bind[ComplexApiType], client.bindPositional[ComplexApiType]),
//          Seq(client.bind[InvalidApiType], client.bindPositional[InvalidApiType]),
//          (method, p1) => client.method(method).positional.args(p1).call,
//          (method, p1) => client.method(method).args(p1).call,
//          (method, p1) => client.method(method).positional.args(p1).tell,
//          (method, p1) => client.method(method).args(p1).tell
//        )
//      }, {
//        val port = availablePort
//        val codec = UpickleMessagePackCodec(ProtocolCodecSpec)
//        val protocol = JsonRpcProtocol(codec)
//        val handler =
//          Handler[UpickleMessagePackCodec.Node, UpickleMessagePackCodec[ProtocolCodecSpec.type], Effect, Context](
//            codec,
//            system,
//            protocol
//          )
//            .bind(simpleApiInstance).bind(complexApiInstance)
//        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
//        val client = Client[UpickleMessagePackCodec.Node, UpickleMessagePackCodec[ProtocolCodecSpec.type], Effect, Context](codec, system, transport, protocol)
//        TestFixture(
//          codec.getClass,
//          client,
//          handler,
//          port,
//          Seq(client.bind[SimpleApiType], client.bindPositional[SimpleApiType]),
//          Seq(client.bind[ComplexApiType], client.bindPositional[ComplexApiType]),
//          Seq(client.bind[InvalidApiType], client.bindPositional[InvalidApiType]),
//          (method, p1) => client.method(method).positional.args(p1).call,
//          (method, p1) => client.method(method).args(p1).call,
//          (method, p1) => client.method(method).positional.args(p1).tell,
//          (method, p1) => client.method(method).args(p1).tell
//        )
//      }, {
//        implicit lazy val enumCodecJson: CodecJson[Enum.Enum] = CodecJson(
//          (v: Enum.Enum) => jNumber(Enum.toOrdinal(v)),
//          cursor => cursor.focus.as[Int].map(Enum.fromOrdinal)
//        )
//        implicit lazy val structureCodecJson: CodecJson[Structure] =
//          Argonaut.codec1(Structure.apply, (v: Structure) => v.value)("value")
//        implicit lazy val recordCodecJson: CodecJson[Record] =
//          Argonaut.codec13(
//            Record.apply,
//            (v: Record) =>
//              (
//                v.string,
//                v.boolean,
//                v.byte,
//                v.short,
//                v.int,
//                v.long,
//                v.float,
//                v.double,
//                v.enumeration,
//                v.list,
//                v.map,
//                v.structure,
//                v.none
//              )
//          )(
//            "string",
//            "boolean",
//            "byte",
//            "short",
//            "int",
//            "long",
//            "float",
//            "double",
//            "enumeration",
//            "list",
//            "map",
//            "structure",
//            "none"
//          )
//        val port = availablePort
//        val codec = ArgonautJsonCodec()
//        val protocol = JsonRpcProtocol(codec)
//        val handler = Handler[ArgonautJsonCodec.Node, ArgonautJsonCodec, Effect, Context](codec, system, protocol)
//          .bind(simpleApiInstance).bind(complexApiInstance)
//        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
//        val client = Client[ArgonautJsonCodec.Node, ArgonautJsonCodec, Effect, Context](codec, system, transport, protocol)
//        TestFixture(
//          codec.getClass,
//          client,
//          handler,
//          port,
//          Seq(client.bind[SimpleApiType], client.bindPositional[SimpleApiType]),
//          Seq(client.bind[ComplexApiType], client.bindPositional[ComplexApiType]),
//          Seq(client.bind[InvalidApiType], client.bindPositional[InvalidApiType]),
//          (method, p1) => client.method(method).positional.args(p1).call,
//          (method, p1) => client.method(method).args(p1).call,
//          (method, p1) => client.method(method).positional.args(p1).tell,
//          (method, p1) => client.method(method).args(p1).tell
//        )
      }
    )
  }

  override def fixtures: Seq[TestFixture] = testFixtures

  def customTransport(index: Int): Option[ClientMessageTransport[Effect, Context]] = None

  private def contextValue: Context = arbitraryContext.arbitrary.sample.get
}

object ProtocolCodecSpec extends UpickleCustom {

  implicit def enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit def structureRw: ReadWriter[Structure] = macroRW
  implicit def recordRw: ReadWriter[Record] = macroRW
}
