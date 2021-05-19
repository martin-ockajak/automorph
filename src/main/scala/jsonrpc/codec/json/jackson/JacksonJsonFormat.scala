package jsonrpc.codec.json.jackson

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import jsonrpc.spi.{Codec, Message}
import scala.collection.immutable.ArraySeq
import scala.compiletime.summonInline
import scala.reflect.ClassTag

final case class JacksonJsonFormat(mapper: ObjectMapper = JacksonJsonFormat.defaultMapper)
  extends Codec[JsonNode]:
  type DOM = JsonNode

  def serialize(message: Message[DOM]): ArraySeq.ofByte =
    ArraySeq.ofByte(mapper.writeValueAsBytes(message).nn)

  def derialize(json: ArraySeq.ofByte): Message[DOM] =
    mapper.readValue(json.unsafeArray, classOf[Message[DOM]]).nn

  def format(message: Message[DOM]): String =
    mapper.writerWithDefaultPrettyPrinter.nn.writeValueAsString(message).nn

  inline def encode[T](value: T): DOM =
    mapper.valueToTree(value).asInstanceOf[DOM]

  inline def decode[T](json: DOM): T =
    val classTag = summonInline[ClassTag[T]]
    val valueClass = classTag.runtimeClass.asInstanceOf[Class[T]]
    mapper.treeToValue(json, valueClass).nn

object JacksonJsonFormat:
  def defaultMapper: ObjectMapper =
    JsonMapper.builder.nn.addModule(DefaultScalaModule).nn.build.nn
