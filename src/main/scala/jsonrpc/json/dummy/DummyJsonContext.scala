package jsonrpc.json.dummy

import java.nio.charset.StandardCharsets
import jsonrpc.spi.{JsonContext, Message}
import scala.collection.immutable.ArraySeq
import DummyJsonContext.*

case object DummyJsonContext:
  private type Json = String

  final case class Encoder[T]():
    def encode(t:T):String = t.toString

  @FunctionalInterface
  abstract class Decoder[T]:
    def decode(json:String):T

  // this 2 givens automatically causes
  // .encode[_]()
  // .decode[String]()
  // to type check (without local givens)
  // because givens defined in companion objects are implicitely available
  given [T]: Encoder[T] = Encoder[T]()

  // for test purposes, only strings are decoded
  given Decoder[String] = identity


final case class DummyJsonContext()
  extends JsonContext[Json, Encoder, Decoder]:

  private val charset = StandardCharsets.UTF_8.nn

  def serialize(message: Message[Json]): ArraySeq.ofByte =
    val array = message.toString.getBytes(charset).nn
    ArraySeq.ofByte( array)

  def derialize(json: ArraySeq.ofByte): Message[Json] = Message(None, None, None, None, None, None)

  def format(message: Message[Json]): String = message.toString

  def encode[T: Encoder](value: T): Json = summon[Encoder[T]].encode(value)

  def decode[T: Decoder](json: Json): T = summon[Decoder[T]].decode(json)
