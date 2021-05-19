package jsonrpc.effect.standard

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.EffectContext
import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try, Failure}

final case class FutureEffect(executionContext: ExecutionContext)
  extends EffectContext[Future]:
  
  private given ExecutionContext = executionContext

  def pure[T](value: T): Future[T] =
    Future.successful(value)

  def map[T, R](value: Future[T], function: T => R): Future[R] =
    value.map(function)

  def either[T](value: Future[T]): Future[Either[Throwable, T]] =
    value.transform(value => Success(value.toEither))
