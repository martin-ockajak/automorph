package jsonrpc.json.jackson

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.github.gaeljw.typetrees.TypeTreeTag
import io.github.gaeljw.typetrees.TypeTreeTagMacros.typeTreeTag
import jsonrpc.spi.{JsonContext, Message}
import scala.collection.immutable.ArraySeq

final case class JacksonJsonContext(mapper: ObjectMapper = JacksonJsonContext.defaultMapper)
  extends JsonContext[JsonNode]:
  type Json = JsonNode

  def serialize(message: Message[Json]): ArraySeq.ofByte =
    ArraySeq.ofByte(mapper.writeValueAsBytes(message).nn)

  def derialize(json: ArraySeq.ofByte): Message[Json] =
    mapper.readValue(json.unsafeArray, classOf[Message[Json]]).nn

  def format(message: Message[Json]): String =
    mapper.writerWithDefaultPrettyPrinter.nn.writeValueAsString(message).nn

  def encode[T](value: T): Json =
    mapper.valueToTree(value).nn

  inline def decode[T](json: Json): T = ???
//    val tag: TypeTreeTag = typeTreeTag[T]
//    val valueClass = tag.self.runtimeClass.asInstanceOf[Class[T]]
//    mapper.treeToValue(json, valueClass).nn

object JacksonJsonContext:
  def defaultMapper: ObjectMapper =
    JsonMapper.builder.nn.addModule(DefaultScalaModule).nn.build.nn
