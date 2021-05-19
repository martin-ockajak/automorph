package jsonrpc.codec.json.dummy

import java.nio.charset.StandardCharsets
import jsonrpc.spi.{Codec, Message}
import scala.collection.immutable.ArraySeq

final case class DummyJsonFormat()
  extends Codec[String]:
  type DOM = String

  private val charset = StandardCharsets.UTF_8.nn

  def serialize(message: Message[DOM]): ArraySeq.ofByte =
    ArraySeq.ofByte(message.toString.getBytes(charset).nn)

  def derialize(json: ArraySeq.ofByte): Message[DOM] =
    Message(None, None, None, None, None, None)

  def format(message: Message[DOM]): String =
    message.toString

  inline def encode[T](value: T): DOM =
    value.toString

  inline def decode[T](json: DOM): T =
    json.asInstanceOf[T]
