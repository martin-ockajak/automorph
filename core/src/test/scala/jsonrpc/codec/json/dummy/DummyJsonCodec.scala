package jsonrpc.codec.json.dummy

import jsonrpc.util.EncodingOps.toArraySeq
import jsonrpc.spi.{Codec, Message}

import scala.collection.immutable.ArraySeq

final case class DummyJsonCodec() extends Codec[String]:

  override def mimeType: String = "text/plain"

  override def serialize(message: Message[String]): ArraySeq.ofByte =
    message.toString.toArraySeq

  override def deserialize(data: ArraySeq.ofByte): Message[String] =
    Message(None, None, None, None, None, None)

  override def format(message: Message[String]): String =
    message.toString

  override inline def encode[T](value: T): String =
    value.toString

  override inline def decode[T](node: String): T =
    node.asInstanceOf[T]
