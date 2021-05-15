package jsonrpc

import base.BaseSpec
import jsonrpc.effect.native.PlainEffectContext
import jsonrpc.json.dummy.DummyJsonContext
import jsonrpc.spi.Message
import jsonrpc.spi.CallError
//import upickle.default.*
//import ujson.Value
//import ujson.Str
import io.circe.syntax.*
import io.circe.parser.decode
import io.circe.*
import io.circe.generic.semiauto.*
//import io.circe.generic.auto.*

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
  private val message = Message[String](
    Some("2.0"),
    None,
    None,
    None,
    Some("test"),
    None
  )
//  private val upickleTest = UpickleTest(
//    Some("2.0"),
//    None,
//    None,
//    None,
//    Some(Str("test"))
//  )
  private val circeTest = CirceTest(
    Some("2.0"),
    None,
    None,
    None,
    Some(Json.fromString("test"))
  )
//  private given enumRw: ReadWriter[Enum] = upickle.default.readwriter[Int].bimap[Enum](
//    value => value.ordinal,
//    number => Enum.fromOrdinal(number)
//  )
//  private given ReadWriter[Structure] = macroRW
//  private given ReadWriter[Record] = macroRW
//  private given ReadWriter[CallError[Value]] = macroRW
//  private given ReadWriter[Message[Value]] = macroRW
//  private given ReadWriter[UpickleTest] = macroRW
  given Encoder[Thing] = deriveEncoder[Thing]
//  given Decoder[Thing] = deriveDecoder[Thing]
//  private given Encoder[Enum] = Encoder.encodeInt.contramap[Enum](_.ordinal)
//  private given Decoder[Enum] = Decoder.decodeInt.map(Enum.fromOrdinal)
//  private given Encoder[Structure] = deriveEncoder[Structure]
//  private given Decoder[Structure] = deriveDecoder[Structure]
//  private given Encoder[Record] = deriveEncoder[Record]
//  private given Decoder[Record] = deriveDecoder[Record]
//  private given Encoder[CirceTest] = deriveEncoder[CirceTest]
//  private given Decoder[CirceTest] = deriveDecoder[CirceTest]

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
//      "Upickle" in {
//        val recordJson = upickle.default.write(record)
//        println(upickle.default.read[Record](recordJson))
//        val upickleTestJson = upickle.default.write(upickleTest)
//        println(upickle.default.read[UpickleTest](upickleTestJson))
//        println(upickle.default.write(message))
//      }
      "Circe" in {
        val thing = Thing("test")
        val thingJson = thing.asJson.spaces2
        println(thingJson)
//        println(decode[Thing](thingJson))
//        val structureJson = structure.asJson.spaces2
//        println(structureJson)
//        println(decode[Structure](structureJson))
//        val recordJson = record.asJson.spaces2
//        println(recordJson)
//        println(decode[Record](recordJson))
//        val circeTestJson = circeTest.asJson.spaces2
//        println(circeTestJson)
//        println(decode[CirceTest](circeTestJson))
      }
    }
  }

//  final case class UpickleTest(
//    jsonrpc: Option[String],
//    id: Option[Either[BigDecimal, String]],
//    method: Option[String],
//    params: Option[Either[List[Value], Map[String, Value]]],
//    result: Option[Value]
//  )

final case class CirceTest(
  jsonrpc: Option[String],
  id: Option[Either[BigDecimal, String]],
  method: Option[String],
  params: Option[Either[List[Json], Map[String, Json]]],
  result: Option[Json]
)

final case class Thing(
  value: String
)
