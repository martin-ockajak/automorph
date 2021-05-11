package jsonrpc.spi

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import scala.collection.immutable.ArraySeq

trait BaseJsonRpcServer[Effect[_]]:
  def bind[T <: AnyRef](api: T): Unit

  def process(request: ArraySeq[Byte]): Effect[ArraySeq[Byte]]

  def process(request: InputStream): Effect[InputStream]

  def process(request: ByteBuffer): Effect[ByteBuffer]
