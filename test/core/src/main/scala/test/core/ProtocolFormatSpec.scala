package test.core

import argonaut.Argonaut.jNumber
import argonaut.{Argonaut, CodecJson}
import automorph.format.UpickleCustom
import automorph.format.json.{ArgonautJsonFormat, CirceJsonFormat, UpickleJsonFormat}
import automorph.format.messagepack.UpickleMessagePackFormat
import automorph.spi.transport.ClientMessageTransport
import automorph.transport.local.client.HandlerTransport
import automorph.{Client, Handler}
import io.circe.generic.auto.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import test.core.CoreSpec
import test.{Enum, Record, Structure}

trait ProtocolFormatSpec extends CoreSpec {

  private lazy val testFixtures = {
    implicit val usingContext: Context = contextValue
    Seq(
      {
        implicit lazy val enumEncoder: Encoder[Enum.Enum] = Encoder.encodeInt.contramap[Enum.Enum](Enum.toOrdinal)
        implicit lazy val enumDecoder: Decoder[Enum.Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
        implicit lazy val structureEncoder: Encoder[Structure] = deriveEncoder[Structure]
        implicit lazy val structureDecoder: Decoder[Structure] = deriveDecoder[Structure]
        val port = availablePort
        val format = CirceJsonFormat()
        val handler = Handler[CirceJsonFormat.Node, CirceJsonFormat, Effect, Context](format, system)
          .bind(simpleApiInstance).bind(complexApiInstance)
        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
        val client: Client[CirceJsonFormat.Node, CirceJsonFormat, Effect, Context] =
          Client(format, system, transport)
        TestFixture(
          format.getClass,
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
      }, {
        val port = availablePort
        val format = UpickleJsonFormat(ProtocolFormatSpec)
        val handler = Handler[UpickleJsonFormat.Node, UpickleJsonFormat[ProtocolFormatSpec.type], Effect, Context](format, system)
          .bind(simpleApiInstance).bind(complexApiInstance)
        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
        val client: Client[UpickleJsonFormat.Node, UpickleJsonFormat[ProtocolFormatSpec.type], Effect, Context] =
          Client(format, system, transport)
        TestFixture(
          format.getClass,
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
      }, {
        val port = availablePort
        val format = UpickleMessagePackFormat(ProtocolFormatSpec)
        val handler = Handler[UpickleMessagePackFormat.Node, UpickleMessagePackFormat[ProtocolFormatSpec.type], Effect, Context](format, system)
          .bind(simpleApiInstance).bind(complexApiInstance)
        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
        val client: Client[UpickleMessagePackFormat.Node, UpickleMessagePackFormat[ProtocolFormatSpec.type], Effect, Context] =
          Client(format, system, transport)
        TestFixture(
          format.getClass,
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
      }, {
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
        val format = ArgonautJsonFormat()
        val handler = Handler[ArgonautJsonFormat.Node, ArgonautJsonFormat, Effect, Context](format, system)
          .bind(simpleApiInstance).bind(complexApiInstance)
        val transport = customTransport(port).getOrElse(HandlerTransport(handler, system, contextValue))
        val client: Client[ArgonautJsonFormat.Node, ArgonautJsonFormat, Effect, Context] =
          Client(format, system, transport)
        TestFixture(
          format.getClass,
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
      }
    )
  }

  override def fixtures: Seq[TestFixture] = testFixtures

  def customTransport(index: Int): Option[ClientMessageTransport[Effect, Context]] = None

  private def contextValue: Context = arbitraryContext.arbitrary.sample.get
}

object ProtocolFormatSpec extends UpickleCustom {

  implicit def enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit def structureRw: ReadWriter[Structure] = macroRW
  implicit def recordRw: ReadWriter[Record] = macroRW
}