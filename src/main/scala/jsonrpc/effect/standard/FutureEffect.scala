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
 * Documentation: https://docs.scala-lang.org/overviews/core/futures.html
 * Effect type: Future
 * Effect type API: https://www.scala-lang.org/api/2.13.6/scala/concurrent/Future.html
 */
final case class FutureEffect()(using ExecutionContext) extends Effect[Future]:

  def pure[T](value: T): Future[T] = Future.successful(value)

  def map[T, R](effect: Future[T], function: T => R): Future[R] = effect.map(function)

  def either[T](effect: Future[T]): Future[Either[Throwable, T]] = effect.transform(value => Success(value.toEither))
