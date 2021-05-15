package jsonrpc

import base.BaseSpec
import jsonrpc.effect.native.PlainEffectContext
import jsonrpc.json.dummy.DummyJsonContext
import jsonrpc.json.upickle.UpickleJsonContext
import jsonrpc.spi.{CallError, Message}
import ujson.{Bool, Num, Str, Value}
import upickle.default.*
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
  private val dummyMessage = DummyJsonContext.Message(
    Some("2.0"),
    None,
    None,
    Some(Right(Map(
      "x" -> "foo",
      "y" -> "1",
      "z" -> "true"
    ))),
    Some("test"),
    None
  )
  private val upickleMessage = UpickleJsonContext.Message(
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
  private given ReadWriter[Structure] = macroRW
  private given ReadWriter[Record] = macroRW
  private given ReadWriter[UpickleJsonContext.CallError] = macroRW
  private given ReadWriter[UpickleJsonContext.Message] = macroRW
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
        val server = JsonRpcServer(DummyJsonContext(), PlainEffectContext())
        val api = ApiImpl("")
        server.bind(api)
        (0 == 0).shouldBe(true)
      }
    }
    "JSON" - {
      "Upickle" in {
        val recordJson = upickle.default.write(record)
        println(upickle.default.read[Record](recordJson))
        val messageJson = upickle.default.write(upickleMessage)
        println(upickle.default.read[UpickleJsonContext.Message](messageJson))
      }
      "Circe" in {
//        val thing = Thing("test")
//        val thingJson = thing.asJson.spaces2
//        println(thingJson)
//        println(decode[Thing](thingJson))
//        val structureJson = structure.asJson.spaces2
//        println(structureJson)
//        println(decode[Structure](structureJson))
//        val recordJson = record.asJson.spaces2
//        println(recordJson)
//        println(decode[Record](recordJson))
      }
    }
  }

final case class Thing(
  value: String
)
