package jsonrpc.format.json.dummy

import java.nio.charset.StandardCharsets
import jsonrpc.spi.{FormatContext, Message}
import scala.collection.immutable.ArraySeq

final case class DummyJsonFormat()
  extends FormatContext[String]:
  type Json = String

  private val charset = StandardCharsets.UTF_8.nn

  def serialize(message: Message[Json]): ArraySeq.ofByte =
    ArraySeq.ofByte(message.toString.getBytes(charset).nn)

  def derialize(json: ArraySeq.ofByte): Message[Json] = Message(None, None, None, None, None, None)

  def format(message: Message[Json]): String = message.toString

  def encode[T](value: T): Json = value.toString

  def decode[T](json: Json): T = json.asInstanceOf[T]
