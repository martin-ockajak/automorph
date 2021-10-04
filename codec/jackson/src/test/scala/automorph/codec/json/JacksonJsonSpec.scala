package automorph.codec.json

import automorph.codec.json.JacksonJsonCodec
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.{ArrayNode, BooleanNode, IntNode, ObjectNode, TextNode}
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode, SerializerProvider}
import org.scalacheck.{Arbitrary, Gen}
import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}
import test.Generators.arbitraryRecord
import test.codec.json.JsonMessageCodecSpec
import test.{Enum, Record}

class JacksonJsonSpec extends JsonMessageCodecSpec {

  type Node = JsonNode
  type ActualCodec = JacksonJsonCodec
  override lazy val codec: ActualCodec = JacksonJsonCodec(JacksonJsonCodec.defaultMapper.registerModule(customModule))

  override lazy val arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.recursive[Node](recurse =>
    Gen.oneOf(
      Gen.resultOf(TextNode.valueOf),
      Gen.resultOf(IntNode.valueOf),
      Gen.resultOf(BooleanNode.valueOf),
      Gen.listOfN[Node](2, recurse).map((values: List[Node]) =>
        new ArrayNode(JacksonJsonCodec.defaultMapper.getNodeFactory, values.asJava)
      ),
      Gen.mapOfN(2, Gen.zip(Arbitrary.arbitrary[String], recurse)).map(values =>
        new ObjectNode(JacksonJsonCodec.defaultMapper.getNodeFactory, values.asJava)
      )
    )
  ))

  private lazy val enumClass = classOf[Enum.Enum]

  private lazy val enumSerializer = new StdSerializer[Enum.Enum](enumClass) {

    override def serialize(value: Enum.Enum, generator: JsonGenerator, provider: SerializerProvider): Unit =
      generator.writeNumber(Enum.toOrdinal(value))
  }

  private lazy val enumDeserializer = new StdDeserializer[Enum.Enum](enumClass) {

    override def deserialize(parser: JsonParser, context: DeserializationContext): Enum.Enum =
      Enum.fromOrdinal(parser.getIntValue)
  }

  private lazy val customModule = new SimpleModule()
    .addSerializer(enumClass, enumSerializer)
    .addDeserializer(enumClass, enumDeserializer)

  "" - {
    "Encode & Decode" in {
      check { (record: Record) =>
        val encoded = codec.encode(record)
        val decoded = codec.decode[Record](encoded)
        decoded.equals(record)
      }
    }
    "Test" in {
      val message = automorph.protocol.jsonrpc.Message(
        None,
        Some(Left[BigDecimal, String](BigDecimal(1.2))),
        Some("method"),
        Some(Right[List[JsonNode], Map[String, JsonNode]](Map("arg1" -> IntNode.valueOf(1)))),
        None,
        Some(automorph.protocol.jsonrpc.MessageError[JsonNode](Some("message"), None, Some(TextNode.valueOf("data"))))
      )
      println(codec.text(codec.encode(message)))
    }
  }
}
