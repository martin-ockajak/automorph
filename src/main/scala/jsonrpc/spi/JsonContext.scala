package jsonrpc.spi

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.Message
import scala.collection.immutable.ArraySeq

trait JsonContext[JsonValue]:

  def serialize(response: Message[JsonValue]): Array[Byte]

  def derialize(json: Array[Byte]): Message[JsonValue]

  def encode[T](value: T): JsonValue

  def decode[T](json: JsonValue): T
