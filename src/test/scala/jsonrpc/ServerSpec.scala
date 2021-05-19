package jsonrpc

import base.BaseSpec
import jsonrpc.effect.native.PlainEffect
import jsonrpc.format.json.dummy.DummyJsonFormat
import jsonrpc.format.json.upickle.UpickleJsonFormat
import jsonrpc.format.json.jackson.JacksonJsonFormat
import jsonrpc.format.json.upickle.UpickleMacros
import jsonrpc.spi.{CallError, Message}
import ujson.{Bool, Num, Str, Value}
import upickle.default.{Writer, ReadWriter}
//import io.circe.syntax.*
//import io.circe.parser.decode
//import io.circe.*
//import io.circe.generic.semiauto.*

class ServerSpec
  extends BaseSpec:
  private val structure = Structure(
    "test"
  )
  private val record = Record(
    "test",
    boolean = true,
    0,
    1,
    Some(2),
    3,
    4.5,
    6.7,
    Some(Enum.One),
    List("x", "y", "z"),
    Map(
      "foo" -> 0,
      "bar" -> 1
    ),
    Some(structure),
    None
  )
  private val upickleMessage = Message(
    Some("2.0"),
    None,
    None,
    Some(Right(Map(
      "x" -> Str("foo"),
      "y" -> Num(1),
      "z" -> Bool(true)
    ))),
    Some(Str("test")),
    None
  )
//  private val circeTest = CirceTest(
//    Some("2.0"),
//    None,
//    None,
//    None,
//    Some(Json.fromString("test"))
//  )
  private given enumRw: ReadWriter[Enum] = upickle.default.readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
//  given Encoder[Thing] = deriveEncoder[Thing]
//  given Decoder[Thing] = deriveDecoder[Thing]
//  private given Encoder[Enum] = Encoder.encodeInt.contramap[Enum](_.ordinal)
//  private given Decoder[Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
//  private given Encoder[Structure] = deriveEncoder[Structure]
//  private given Decoder[Structure] = deriveDecoder[Structure]
//  private given Encoder[Record] = deriveEncoder[Record]
//  private given Decoder[Record] = deriveDecoder[Record]

  "" - {
    "Bind" - {
      "Default" in {
        val server = JsonRpcServer(DummyJsonFormat(), PlainEffect())
        val api = ApiImpl("")
        server.bind(api)
        (0 == 0).shouldBe(true)
      }
    }
    "JSON" - {
      "Dummy" in {
        val jsonContext = DummyJsonFormat()
        jsonContext.encode("test")
      }
      "Jackson" in {
        val jsonContext = JacksonJsonFormat()
        val test = jsonContext.encode[String]("test")
        println(test)
        println(jsonContext.decode[String](test))
      }
      "Upickle" in {
        val jsonContext = UpickleJsonFormat()
        println(UpickleMacros.xencode(jsonContext, "test"))
//        println(jsonContext.encode("test"))
//        val messageJson = jsonContext.serialize(upickleMessage)
//        println(jsonContext.derialize(messageJson))
//        println(jsonContext.format(upickleMessage))
      }
    }
  }

final case class Thing(
  value: String
)
