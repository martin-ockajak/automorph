package jsonrpc.codec.json.dummy

import jsonrpc.core.EncodingOps.{asArraySeq, toArray}
import jsonrpc.spi.{Codec, Message}

import scala.collection.immutable.ArraySeq

final case class DummyJsonCodec()
  extends Codec[String] :

  def serialize(message: Message[String]): ArraySeq.ofByte =
    message.toString.toArray.asArraySeq

  def deserialize(data: ArraySeq.ofByte): Message[String] =
    Message(None, None, None, None, None, None)

  def format(message: Message[String]): String =
    message.toString

  inline def encode[T](value: T): String =
    value.toString

  inline def decode[T](node: String): T =
    node.asInstanceOf[T]
