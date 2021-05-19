package jsonrpc

import base.BaseSpec
import jsonrpc.effect.native.PlainEffect
import jsonrpc.codec.json.jackson.JacksonJsonCodec
import jsonrpc.codec.json.dummy.DummyJsonCodec
import jsonrpc.codec.json.upickle.UpickleJsonFormat
import jsonrpc.spi.{CallError, Message}
import com.fasterxml.jackson.databind.JsonNode
import ujson.{Bool, Num, Str}
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
  private val jacksonMessage = Message[JsonNode](
    Some("2.0"),
    None,
    None,
    None,
    None,
    None
  )
  private given enumRw: ReadWriter[Enum] = upickle.default.readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )

  "" - {
    "Bind" - {
      "Default" in {
        val server = JsonRpcServer(DummyJsonCodec(), PlainEffect())
        val api = ApiImpl("")
        server.bind(api)
        (0 == 0).shouldBe(true)
      }
    }
    "JSON" - {
      "Dummy" in {
        val jsonContext = DummyJsonCodec()
        jsonContext.encode("test")
      }
      "Jackson" in {
        val jsonContext = JacksonJsonCodec()
        val valueJson = jsonContext.encode("test")
        println(valueJson)
        println(jsonContext.decode[String](valueJson))
        val messageJson = jsonContext.serialize(jacksonMessage)
        println(jsonContext.derialize(messageJson))
        println(jsonContext.format(jacksonMessage))
      }
      "Upickle" in {
        val jsonContext = UpickleJsonFormat(upickle.default)
        println(jsonContext.encode("test"))
//        val messageJson = jsonContext.serialize(upickleMessage)
//        println(messageJson)
//        println(jsonContext.derialize(messageJson))
//        println(jsonContext.format(upickleMessage))
      }
    }
  }

final case class Thing(
  value: String
)
