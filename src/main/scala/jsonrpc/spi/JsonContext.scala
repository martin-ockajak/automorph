package jsonrpc.spi

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.Message
import scala.collection.immutable.ArraySeq

trait JsonContext[Json, Encoder[_], Decoder[_]]:
  
  def serialize(message: Message[Json]): Array[Byte]

  def derialize(json: Array[Byte]): Message[Json]

  def format(message: Message[Json]): String

  def encode[T: Encoder](value: T): Json

  def decode[T: Decoder](json: Json): T
