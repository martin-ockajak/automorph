package jsonrpc.backend

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.Backend
import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * Future effect backend plugin.
 *
 * @see [[https://docs.scala-lang.org/overviews/core/futures.html Documentation]]
 * @see Effect type: [[scala.concurrent.Future]]
 * @param executionContext execution context
 */
final case class FutureBackend()(implicit executionContext: ExecutionContext) extends Backend[Future] {

  override def pure[T](value: T): Future[T] = Future.successful(value)

  override def failed[T](exception: Throwable): Future[T] = Future.failed(exception)

  override def flatMap[T, R](effect: Future[T], function: T => Future[R]): Future[R] = effect.flatMap(function)

  override def either[T](effect: Future[T]): Future[Either[Throwable, T]] = effect.transform(value => Success(value.toEither))
}
