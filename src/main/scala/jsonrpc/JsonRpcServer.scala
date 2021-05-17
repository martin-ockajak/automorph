package jsonrpc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import jsonrpc.server.ServerMacros
import jsonrpc.spi.{EffectContext, JsonContext}
import scala.collection.immutable.ArraySeq

final case class JsonRpcServer[Json, Encoder[_], Decoder[_], Effect[_]](
  jsonContext: JsonContext[Json, Encoder, Decoder],
  effectContext: EffectContext[Effect]):

  private val charset = StandardCharsets.UTF_8.nn
  private val bufferSize = 4096

  inline def bind[T <: AnyRef](api: T): Unit =
    ServerMacros.bind(api)

  def process(request: ArraySeq.ofByte): Effect[ArraySeq.ofByte] =
    val text = new String(request.unsafeArray, charset)
    effectContext.map(
      process(text),
      response =>
        val array:Array[Byte] = response.getBytes(charset).nn

        // TODO: delete me
        //       semantics looked somewhat unclear in API        
        //       inspection of stdlib sources suggests this does not copy the array
        //       test in REPL confermed the array is NOT copied, but unsafely wrapped
        //       (which is what we want for performance)  
        ArraySeq.ofByte(array)
    )


  def process(request: ByteBuffer): Effect[ByteBuffer] =
    val text = charset.decode(request).toString
    effectContext.map(
      process(text),
      response => charset.encode(response)
    )
    effectContext.pure(request)

  def process(request: InputStream): Effect[InputStream] =
    val outputStream = new ByteArrayOutputStream
    val buffer = Array.ofDim[Byte](bufferSize)
    while
      val length = request.read(buffer)
      if length >= 0 then
        outputStream.write(buffer, 0, length)
        true
      else
        false
    do ()
    val text = outputStream.toString(charset.name)
    effectContext.map(
      process(text),
      response => new ByteArrayInputStream(response.getBytes(charset).nn)
    )

  private def process(request: String): Effect[String] =
    effectContext.pure(request)
