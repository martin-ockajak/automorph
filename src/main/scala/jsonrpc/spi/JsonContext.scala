package jsonrpc.spi

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.Message.{Request, Response}
import scala.collection.immutable.ArraySeq

trait JsonContext[JsonValue]:

  def serialize(response: Response[JsonValue]): String

  def derialize(json: String): Request[JsonValue]

  def encode[T](value: T): JsonValue

  def decode[T](json: JsonValue): T
