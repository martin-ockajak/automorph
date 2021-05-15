package jsonrpc.json

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.JsonContext
import scala.collection.immutable.ArraySeq

final case class DummyJsonContext()
  extends JsonContext[String]:
  
  def serialize(response: Response[String]): String = ""

  def derialize(json: String): Request[String] = Request(None, None, None, None)

  def encode[T](value: T): String = ""

  def decode[T](json: String): T = ???
