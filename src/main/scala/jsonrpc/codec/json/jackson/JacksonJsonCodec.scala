package jsonrpc.codec.json.jackson

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.github.gaeljw.typetrees.TypeTreeTagMacros.typeTreeTag
import jsonrpc.spi.{Codec, Message}
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline
import scala.reflect.ClassTag

/**
 * Jackson JSON codec plugin.
 *
 * @see [[https://github.com/FasterXML/jackson Documentation]]
 * @see [[https://fasterxml.github.io/jackson-databind/javadoc/2.12/com/fasterxml/jackson/databind/JsonNode.html Node type]]
 */
final case class JacksonJsonCodec(mapper: ObjectMapper = JacksonJsonCodec.defaultMapper) extends Codec[JsonNode]:

  def serialize(message: Message[JsonNode]): ArraySeq.ofByte = ArraySeq.ofByte(mapper.writeValueAsBytes(message))

  def deserialize(data: ArraySeq.ofByte): Message[JsonNode] =
    mapper.readValue(data.unsafeArray, classOf[Message[JsonNode]])

  def format(message: Message[JsonNode]): String =
    mapper.writerWithDefaultPrettyPrinter.writeValueAsString(message)

  def encode[T](value: T): JsonNode = ???
  // mapper.valueToTree(value).asInstanceOf[JsonNode]

  def decode[T](node: JsonNode): T = ???
  // val valueClass = typeTreeTag[T].self.runtimeClass.asInstanceOf[Class[T]]
  // mapper.treeToValue(node, valueClass)

case object JacksonJsonCodec:

  def defaultMapper: ObjectMapper = JsonMapper.builder.addModule(DefaultScalaModule).build
