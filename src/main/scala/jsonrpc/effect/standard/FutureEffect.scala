package jsonrpc.effect.standard

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.Effect
import jsonrpc.core.ScalaSupport.*
import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try, Failure}

final case class FutureEffect()(using ExecutionContext)
  extends Effect[Future]:

  def pure[T](value: T): Future[T] =
    value.asCompletedFuture

  def map[T, R](effect: Future[T], function: T => R): Future[R] =
    effect.map(function)

  def either[T](value: Future[T]): Future[Either[Throwable, T]] =
    value.transform {
      (value: Try[T]) => value.toEither.asSuccess
    }
