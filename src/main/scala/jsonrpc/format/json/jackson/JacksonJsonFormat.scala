package jsonrpc.format.json.jackson

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import jsonrpc.spi.{FormatContext, Message}
import scala.collection.immutable.ArraySeq
import io.github.gaeljw.typetrees.TypeTreeTagMacros.typeTreeTag

final case class JacksonJsonFormat(mapper: ObjectMapper = JacksonJsonFormat.defaultMapper)
  extends FormatContext[JsonNode]:
  type Json = JsonNode

  def serialize(message: Message[Json]): ArraySeq.ofByte =
    ArraySeq.ofByte(mapper.writeValueAsBytes(message).nn)

  def derialize(json: ArraySeq.ofByte): Message[Json] =
    mapper.readValue(json.unsafeArray, classOf[Message[Json]]).nn

  def format(message: Message[Json]): String =
    mapper.writerWithDefaultPrettyPrinter.nn.writeValueAsString(message).nn

  inline def encode[T](value: T): Json =
    mapper.valueToTree(value).nn

  inline def decode[T](json: Json): T =
    val typeTag = typeTreeTag[T]
    val valueClass = typeTag.self.runtimeClass.asInstanceOf[Class[T]]
    mapper.treeToValue(json, valueClass).nn

object JacksonJsonFormat:
  def defaultMapper: ObjectMapper =
    JsonMapper.builder.nn.addModule(DefaultScalaModule).nn.build.nn
