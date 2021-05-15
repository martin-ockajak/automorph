package jsonrpc.json.dummy

import java.nio.charset.StandardCharsets
import jsonrpc.spi.Message
import jsonrpc.spi.{JsonContext, Message}

final case class DummyJsonContext()
  extends JsonContext[String]:
  private val charset = StandardCharsets.UTF_8.nn

  def serialize(response: Message[String]): Array[Byte] = "".getBytes(charset).nn

  def derialize(json: Array[Byte]): Message[String] = Message(None, None, None, None, None, None)

  def encode[T](value: T): String = value.toString

  def decode[T](json: String): T = ???
