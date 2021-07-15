package test

import argonaut.Argonaut.jNumber
import argonaut.{Argonaut, CodecJson}
import automorph.codec.common.UpickleCustom
import automorph.codec.json.{ArgonautJsonFormat, CirceJsonFormat, UpickleJsonFormat}
import automorph.codec.messagepack.UpickleMessagePackFormat
import automorph.spi.ClientMessageTransport
import automorph.transport.local.client.HandlerTransport
import automorph.{Client, Handler}
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

trait FormatCoreSpec extends CoreSpec {

  def customTransport: Option[ClientMessageTransport[Effect, Context]] = None

  def codecFixtures: Seq[FormatFixture] = {
    implicit val usingContext: Context = contextValue
    Seq(
      {
        val codec = UpickleJsonFormat(FormatCoreSpec)
        val handler = Handler[UpickleJsonFormat.Node, UpickleJsonFormat[FormatCoreSpec.type], Effect, Context](codec, backend)
          .bind(simpleApiInstance).bind(complexApiInstance)
        val transport = customTransport.getOrElse(HandlerTransport(handler, backend, contextValue))
        val client: Client[UpickleJsonFormat.Node, UpickleJsonFormat[FormatCoreSpec.type], Effect, Context] =
          Client(codec, backend, transport)
        FormatFixture(
          codec.getClass,
          client,
          handler,
          Seq(client.bind[SimpleApiType], client.bindPositional[SimpleApiType]),
          Seq(client.bind[ComplexApiType], client.bindPositional[ComplexApiType]),
          Seq(client.bind[InvalidApiType], client.bindPositional[InvalidApiType]),
          (method, p1) => client.method(method).positional.args(p1).call,
          (method, p1) => client.method(method).args(p1).call,
          (method, p1) => client.method(method).positional.args(p1).tell,
          (method, p1) => client.method(method).args(p1).tell
        )
      }, {
        val codec = UpickleMessagePackFormat(FormatCoreSpec)
        val handler = Handler[UpickleMessagePackFormat.Node, UpickleMessagePackFormat[FormatCoreSpec.type], Effect, Context](codec, backend)
          .bind(simpleApiInstance).bind(complexApiInstance)
        val transport = customTransport.getOrElse(HandlerTransport(handler, backend, contextValue))
        val client: Client[UpickleMessagePackFormat.Node, UpickleMessagePackFormat[FormatCoreSpec.type], Effect, Context] =
          Client(codec, backend, transport)
        FormatFixture(
          codec.getClass,
          client,
          handler,
          Seq(client.bind[SimpleApiType], client.bindPositional[SimpleApiType]),
          Seq(client.bind[ComplexApiType], client.bindPositional[ComplexApiType]),
          Seq(client.bind[InvalidApiType], client.bindPositional[InvalidApiType]),
          (method, p1) => client.method(method).positional.args(p1).call,
          (method, p1) => client.method(method).args(p1).call,
          (method, p1) => client.method(method).positional.args(p1).tell,
          (method, p1) => client.method(method).args(p1).tell
        )
      }, {
        implicit lazy val enumEncoder: Encoder[Enum.Enum] = Encoder.encodeInt.contramap[Enum.Enum](Enum.toOrdinal)
        implicit lazy val enumDecoder: Decoder[Enum.Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
        implicit lazy val structureEncoder: Encoder[Structure] = deriveEncoder[Structure]
        implicit lazy val structureDecoder: Decoder[Structure] = deriveDecoder[Structure]
        val codec = CirceJsonFormat()
        val handler = Handler[CirceJsonFormat.Node, CirceJsonFormat, Effect, Context](codec, backend)
          .bind(simpleApiInstance).bind(complexApiInstance)
        val transport = customTransport.getOrElse(HandlerTransport(handler, backend, contextValue))
        val client: Client[CirceJsonFormat.Node, CirceJsonFormat, Effect, Context] =
          Client(codec, backend, transport)
        FormatFixture(
          codec.getClass,
          client,
          handler,
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
        val codec = ArgonautJsonFormat()
        val handler = Handler[ArgonautJsonFormat.Node, ArgonautJsonFormat, Effect, Context](codec, backend)
          .bind(simpleApiInstance).bind(complexApiInstance)
        val transport = customTransport.getOrElse(HandlerTransport(handler, backend, contextValue))
        val client: Client[ArgonautJsonFormat.Node, ArgonautJsonFormat, Effect, Context] =
          Client(codec, backend, transport)
        FormatFixture(
          codec.getClass,
          client,
          handler,
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

  private def contextValue: Context = arbitraryContext.arbitrary.sample.get
}

object FormatCoreSpec extends UpickleCustom {

  implicit def enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit def structureRw: ReadWriter[Structure] = macroRW
  implicit def recordRw: ReadWriter[Record] = macroRW
}
