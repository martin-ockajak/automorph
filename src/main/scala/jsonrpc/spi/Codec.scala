package jsonrpc.spi

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.Message
import scala.collection.immutable.ArraySeq

trait Codec[DOM]:
  def serialize(message: Message[DOM]): ArraySeq.ofByte

  def derialize(json: ArraySeq.ofByte): Message[DOM]

  def format(message: Message[DOM]): String

  inline def encode[T](value: T): DOM

  inline def decode[T](json: DOM): T
