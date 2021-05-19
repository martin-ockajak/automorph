package jsonrpc.codec.json.dummy

import jsonrpc.spi.{Codec, Message}

import scala.collection.immutable.ArraySeq
import jsonrpc.core.ScalaSupport.*

final case class DummyJsonCodec()
  extends Codec[String] :

  def serialize(message: Message[String]): ArraySeq.ofByte =
    message.toString.encodeToBytes.asArraySeq

  def deserialize(json: ArraySeq.ofByte): Message[String] =
    Message(None, None, None, None, None, None)

  def format(message: Message[String]): String =
    message.toString

  inline def encode[T](value: T): String =
    value.toString

  inline def decode[T](json: String): T =
    json.asInstanceOf[T]
