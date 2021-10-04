package automorph.codec.json

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ClassTagExtensions
import scala.collection.immutable.ArraySeq

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

  /** Default Jackson object mapper. */
  lazy val defaultMapper: ObjectMapper = (new ObjectMapper() with ClassTagExtensions)
    .registerModule(DefaultScalaModule)
    .registerModule(JacksonJsonRpc.module)
    .registerModule(JacksonRestRpc.module)

  /** Message node type. */
  type Node = JsonNode
}
