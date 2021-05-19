package jsonrpc.codec.json.dummy

import java.nio.charset.StandardCharsets
import jsonrpc.spi.{Codec, Message}
import scala.collection.immutable.ArraySeq

final case class DummyJsonCodec()
  extends Codec[String]:

  private val charset = StandardCharsets.UTF_8.nn

  def serialize(message: Message[String]): ArraySeq.ofByte =
    ArraySeq.ofByte(message.toString.getBytes(charset).nn)

  def derialize(json: ArraySeq.ofByte): Message[String] =
    Message(None, None, None, None, None, None)

  def format(message: Message[String]): String =
    message.toString

  inline def encode[T](value: T): String =
    value.toString

  inline def decode[T](json: String): T =
    json.asInstanceOf[T]
