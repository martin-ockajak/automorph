package jsonrpc.json

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.JsonContext
import scala.collection.immutable.ArraySeq

final case class DummyJsonContext()
  extends JsonContext:
  
  def serialize[T](value: T): String = ""

  def derialize(json: String): Any = ""
