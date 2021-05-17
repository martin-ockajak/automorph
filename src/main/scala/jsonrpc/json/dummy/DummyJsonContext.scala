package jsonrpc.json.dummy

import java.nio.charset.StandardCharsets
import jsonrpc.spi.{JsonContext, Message}

final case class DummyJsonContext()
  extends JsonContext[String, [_] =>> Unit, [_] =>> Unit]:

  type Json = String
  type Encoder = [_] =>> Unit
  type Decoder = [_] =>> Unit

  private val charset = StandardCharsets.UTF_8.nn

  def serialize(message: Message[Json]): Array[Byte] = message.toString.getBytes(charset).nn

  def derialize(json: Array[Byte]): Message[Json] = Message(None, None, None, None, None, None)

  def format(message: Message[Json]): String = message.toString

  def encode[T: Encoder](value: T): Json = value.toString

  def decode[T: Decoder](json: Json): T = json.asInstanceOf[T]
