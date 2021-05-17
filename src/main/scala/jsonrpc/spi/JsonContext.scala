package jsonrpc.spi

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.Message
import scala.collection.immutable.ArraySeq

trait JsonContext[Json]:
  def serialize(message: Message[Json]): ArraySeq.ofByte

  def derialize(json: ArraySeq.ofByte): Message[Json]

  def format(message: Message[Json]): String

  def encode[T](value: T): Json

  def decode[T](json: Json): T
