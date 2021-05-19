package jsonrpc.codec.json.jackson

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import jsonrpc.spi.{Codec, Message}
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline
import scala.reflect.ClassTag

final case class JacksonJsonCodec(mapper: ObjectMapper = JacksonJsonCodec.defaultMapper)
  extends Codec[JsonNode]:

  def serialize(message: Message[JsonNode]): ArraySeq.ofByte =
    ArraySeq.ofByte(mapper.writeValueAsBytes(message).nn)

  def deserialize(data: ArraySeq.ofByte): Message[JsonNode] =
    mapper.readValue(data.unsafeArray, classOf[Message[JsonNode]]).nn

  def format(message: Message[JsonNode]): String =
    mapper.writerWithDefaultPrettyPrinter.nn.writeValueAsString(message).nn

  inline def encode[T](value: T): JsonNode =
    mapper.valueToTree(value).asInstanceOf[JsonNode]

  inline def decode[T](node: JsonNode): T =
    val classTag = summonInline[ClassTag[T]]
    val valueClass = classTag.runtimeClass.asInstanceOf[Class[T]]
    mapper.treeToValue(node, valueClass).nn

object JacksonJsonCodec:
  def defaultMapper: ObjectMapper =
    JsonMapper.builder.nn.addModule(DefaultScalaModule).nn.build.nn
