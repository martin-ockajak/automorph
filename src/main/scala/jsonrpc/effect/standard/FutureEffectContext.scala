package jsonrpc.effect.standard

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.EffectContext
import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try, Failure}

final case class FutureEffectContext(executionContext: ExecutionContext)
  extends EffectContext[Future]:

  def unit[T](value: T): Future[T] = Future.successful(value)

  def transform[T](result: Future[T], success: (T) => Unit, failure: (Throwable) => Unit): Future[Unit] =
    given ExecutionContext = executionContext
    result.onComplete {
      case Success(value) => success(value)
      case Failure(exception) => failure(exception)
    }
    Future.successful(())
