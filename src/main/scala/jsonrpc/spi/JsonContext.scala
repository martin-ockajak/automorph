package jsonrpc.spi

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.Message
import scala.collection.immutable.ArraySeq

trait JsonContext[Json]:

  def serialize(response: Message[Json]): Array[Byte]

  def derialize(json: Array[Byte]): Message[Json]

  def encode[T](value: T): Json

  def decode[T](json: Json): T
