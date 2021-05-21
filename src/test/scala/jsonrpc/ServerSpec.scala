package jsonrpc

import base.BaseSpec
import jsonrpc.effect.native.NoEffect
//import jsonrpc.codec.json.jackson.JacksonJsonCodec
import jsonrpc.codec.json.dummy.DummyJsonCodec
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.core.ValueOps.{asRight, asSome}
import jsonrpc.spi.{CallError, Message}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.BooleanNode
import ujson.{Bool, Num, Str}
import upickle.default.{ReadWriter, Writer}

//import io.circe.syntax.*
//import io.circe.parser.decode
//import io.circe.*
//import io.circe.generic.semiauto.*

class ServerSpec extends BaseSpec:

  private val structure = Structure(
    "test"
  )

  private val record = Record(
    "test",
    boolean = true,
    0,
    1,
    2.asSome,
    3,
    4.5,
    6.7,
    Enum.One.asSome,
    List("x", "y", "z"),
    Map(
      "foo" -> 0,
      "bar" -> 1
    ),
    structure.asSome,
    None
  )

  private val upickleMessage = Message(
    "2.0".asSome,
    None,
    None,
    Map(
      "x" -> Str("foo"),
      "y" -> Num(1),
      "z" -> Bool(true)
    ).asRight.asSome,
    Str("test").asSome,
    None
  )

  private val jacksonMessage = Message[JsonNode](
    "2.0".asSome,
    None,
    None,
    Map(
      "x" -> TextNode("foo"),
      "y" -> IntNode(1),
      "z" -> BooleanNode.TRUE
    ).asRight.asSome,
    Some(TextNode("test")),
    None
  )

  private given enumRw: ReadWriter[Enum] =
    upickle.default.readwriter[Int].bimap[Enum](
      value => value.ordinal,
      number => Enum.fromOrdinal(number)
    )

  "" - {
    "Bind" - {
      "Default" in {
        val api = ApiImpl("")
        val handler = JsonRpcHandler(DummyJsonCodec(), NoEffect()).bind(api)
        (0 == 0).shouldBe(true)



      }
    }
    "JSON" - {
      "Dummy" in {
        val jsonContext = DummyJsonCodec()
        jsonContext.encode("test")
      }
//      "Jackson" in {
//        val jsonContext = JacksonJsonCodec()
//        val valueJson = jsonContext.encode("test")
//        println(valueJson)
//        println(jsonContext.decode[String](valueJson))
//        val messageJson = jsonContext.serialize(jacksonMessage)
//        println(jsonContext.deserialize(messageJson))
//        println(jsonContext.format(jacksonMessage))
//      }
      "Upickle" in {
        val jsonContext = UpickleJsonCodec(upickle.default)
        println(jsonContext.encode("test"))
        println(jsonContext.decode[String](jsonContext.encode("test")))
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
