package jsonrpc.effect.standard

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.Effect
import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * Future effect system plugin.
 *
 * @see [[https://docs.scala-lang.org/overviews/core/futures.html Documentation]]
 * @see Effect type: [[scala.concurrent.Future]]
 */
final case class FutureEffect()(using ExecutionContext) extends Effect[Future]:

  def pure[T](value: T): Future[T] = Future.successful(value)

  def failed[T](exception: Throwable): Future[T] = Future.failed(exception)

  def flatMap[T, R](effect: Future[T], function: T => Future[R]): Future[R] = effect.flatMap(function)

  def either[T](effect: Future[T]): Future[Either[Throwable, T]] = effect.transform(value => Success(value.toEither))
