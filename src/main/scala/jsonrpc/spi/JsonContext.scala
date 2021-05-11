package jsonrpc.spi

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import scala.collection.immutable.ArraySeq

trait JsonContext:
  def serialize[T](value: T): String

  def derialize(json: String): Any
