package jsonrpc.json.dummy

import java.nio.charset.StandardCharsets
import jsonrpc.spi.{JsonContext, Message}
import scala.collection.immutable.ArraySeq
import DummyJsonContext.*

case object DummyJsonContext:
  type Json = String
  case class DummyEncoder[T]()
  case class DummyDecoder[T]()

  // this automatically causes any call to
  // .encode() and .decode() to work,
  // because givens defined in a companion object are implicitely available
  // that is, no more 'given Unit = ()'
  given [T]: DummyEncoder[T] = DummyEncoder[T]()
  given [T]: DummyDecoder[T] = DummyDecoder[T]()

final case class DummyJsonContext()
  extends JsonContext[Json, DummyEncoder, DummyDecoder]:

  private val charset = StandardCharsets.UTF_8.nn

  def serialize(message: Message[Json]): ArraySeq.ofByte =
    val array = message.toString.getBytes(charset).nn
    ArraySeq.ofByte( array)

  def derialize(json: ArraySeq.ofByte): Message[Json] = Message(None, None, None, None, None, None)

  def format(message: Message[Json]): String = message.toString

  def encode[T: DummyEncoder](value: T): Json = value.toString

  def decode[T: DummyDecoder](json: Json): T = json.asInstanceOf[T]
