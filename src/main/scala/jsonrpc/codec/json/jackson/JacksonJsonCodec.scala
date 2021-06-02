package jsonrpc.codec.json.jackson

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import jsonrpc.spi.{Codec, Message}
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline
import scala.reflect.ClassTag

/**
 * Jackson JSON codec plugin.
 *
 * @see [[https://github.com/FasterXML/jackson Documentation]]
 * @see [[https://fasterxml.github.io/jackson-databind/javadoc/2.12/com/fasterxml/jackson/databind/JsonNode.html Node type]]
 * @param objectMapper Jackson object mapper instance
 */
final case class JacksonJsonCodec(objectMapper: ObjectMapper = JacksonJsonCodec.defaultMapper) extends Codec[JsonNode]:

  def serialize(message: Message[JsonNode]): ArraySeq.ofByte = ArraySeq.ofByte(objectMapper.writeValueAsBytes(message))

  def deserialize(data: ArraySeq.ofByte): Message[JsonNode] =
    objectMapper.readValue(data.unsafeArray, classOf[Message[JsonNode]])

  def format(message: Message[JsonNode]): String =
    objectMapper.writerWithDefaultPrettyPrinter.writeValueAsString(message)

  inline def encode[T](value: T): JsonNode = objectMapper.valueToTree(value).asInstanceOf[JsonNode]

  inline def decode[T](node: JsonNode): T =
    val classTag = summonInline[ClassTag[T]]
    val valueClass = classTag.runtimeClass.asInstanceOf[Class[T]]
    objectMapper.treeToValue(node, valueClass)

case object JacksonJsonCodec:

  def defaultMapper: ObjectMapper = JsonMapper.builder.addModule(DefaultScalaModule).build
