package automorph.codec.json

import com.fasterxml.jackson.core.{JsonGenerator, JsonParseException, JsonParser, TreeNode}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode, ObjectMapper, SerializerProvider}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ClassTagExtensions
import scala.collection.immutable.ArraySeq
import scala.runtime.BoxedUnit

/**
 * Jackson message codec plugin using JSON format.
 *
 * @see [[https://github.com/FasterXML/jackson Documentation]]
 * @see [[https://fasterxml.github.io/jackson-databind/javadoc/2.12/com/fasterxml/jackson/databind/JsonNode.html Node type]]
 * @constructor Creates a Jackson codec plugin using JSON as message format.
 * @param objectMapper Jackson object mapper
 */
final case class JacksonJsonCodec(
  objectMapper: ObjectMapper = JacksonJsonCodec.defaultMapper
) extends JacksonJsonMeta {

  override def mediaType: String = "application/json"

  def serialize(node: JsonNode): ArraySeq.ofByte =
    new ArraySeq.ofByte(objectMapper.writeValueAsBytes(node))

  def deserialize(data: ArraySeq.ofByte): JsonNode =
    objectMapper.readTree(data.unsafeArray)

  override def text(node: JsonNode): String =
    objectMapper.writerWithDefaultPrettyPrinter.writeValueAsString(node)
}

object JacksonJsonCodec {

  private lazy val unitModule = new SimpleModule().addSerializer(
    classOf[BoxedUnit],
    new StdSerializer[BoxedUnit](classOf[BoxedUnit]) {

      override def serialize(value: BoxedUnit, generator: JsonGenerator, provider: SerializerProvider): Unit =
        generator.writeStartObject()
        generator.writeEndObject()
    }
  ).addDeserializer(
    classOf[BoxedUnit],
    new StdDeserializer[BoxedUnit](classOf[BoxedUnit]) {

      override def deserialize(parser: JsonParser, context: DeserializationContext): BoxedUnit =
        parser.readValueAsTree[TreeNode]() match {
          case _: ObjectNode => BoxedUnit.UNIT
          case _ => throw new JsonParseException(parser, "Invalid unit value", parser.getCurrentLocation)
        }
    }
  )

  /** Default Jackson object mapper. */
  lazy val defaultMapper: ObjectMapper = (new ObjectMapper() with ClassTagExtensions)
    .registerModule(DefaultScalaModule)
    .registerModule(unitModule)
    .registerModule(JacksonJsonRpc.module)
    .registerModule(JacksonRestRpc.module)

  /** Message node type. */
  type Node = JsonNode
}
