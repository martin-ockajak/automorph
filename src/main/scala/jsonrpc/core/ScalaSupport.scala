package jsonrpc.core

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.ByteBuffer
import scala.collection.immutable.ArraySeq
import scala.concurrent.Future
import scala.util.{Try, Success}

case object ScalaSupport:

  private lazy val charset = UTF_8.nn

  extension [X](value:X)
    def some:Option[X] = Some(value)
    def asCompletedFuture:Future[X] = Future.successful(value)
    def asSuccess:Try[X] = Success(value)
    def asLeft[R]  :Either[X, R]  = Left(value)
    def asRight[L] :Either[L, X]  = Right(value)

  extension (bytes:Array[Byte])
    def decodeToString:String = new String(bytes, charset)
    def asArraySeq:ArraySeq.ofByte = ArraySeq.ofByte(bytes)

  extension (str:String)
    def encodeToBytes: Array[Byte] = str.getBytes(charset).nn
    def encodeToByteBuffer: ByteBuffer = charset.encode(str).nn

  extension (buffer: ByteBuffer)
    def decodeToString: String = charset.decode(buffer).toString

  extension (os: ByteArrayOutputStream)
    def decodeToString: String = os.toString(charset.name)

end ScalaSupport


