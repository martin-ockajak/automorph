package jsonrpc.json.upickle

import java.nio.charset.StandardCharsets
import jsonrpc.spi.Message
import jsonrpc.spi.{JsonContext, Message}
import upickle.default.*
import ujson.Value

final case class UpickleJsonContext()
  extends JsonContext[Value]:
  private val charset = StandardCharsets.UTF_8.nn
  def serialize(response: Message[Value]): Array[Byte] = ???

  def derialize(json: Array[Byte]): Message[Value] = ???

  def encode[T](value: T): Value = ???

  def decode[T](json: Value): T = ???

//  def serialize(response: Message[Value]): Array[Byte] = upickle.default.writeBinary(response)

//  def derialize(json: Array[Byte]): Message[Value] = upickle.default.readBinary(json)

//  def encode[T](value: T): Value = upickle.default.writeJs(value)

//  def decode[T](json: Value): T = upickle.default.read[T](json)
