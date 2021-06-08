package jsonrpc.codec.json.dummy

import java.nio.charset.StandardCharsets
import jsonrpc.spi.{Codec, Message}
import scala.collection.immutable.ArraySeq

final case class DummyJsonCodec() extends Codec[String]:

  private val charset = StandardCharsets.UTF_8

  override def mediaType: String = "text/plain"

  override def serialize(message: Message[String]): ArraySeq.ofByte =
    ArraySeq.ofByte(message.toString.getBytes(charset))

  override def deserialize(data: ArraySeq.ofByte): Message[String] =
    Message(None, None, None, None, None, None)

  override def format(message: Message[String]): String =
    message.toString

  override inline def encode[T](value: T): String =
    value.toString

  override inline def decode[T](node: String): T =
    node.asInstanceOf[T]
