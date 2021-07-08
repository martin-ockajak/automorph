package automorph.backend

import automorph.spi.Backend
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

/**
 * Asynchronous backend plugin using Future as an effect type.
 *
 * @see [[https://docs.scala-lang.org/overviews/core/futures.html Documentation]]
 * @see Effect type: [[scala.concurrent.Future]]
 * @constructor Creates an asynchronous backend plugin using `Future` as an effect type.
 * @param executionContext execution context
 */
final case class FutureBackend()(implicit executionContext: ExecutionContext) extends Backend[Future] {

  override def pure[T](value: T): Future[T] = Future.successful(value)

  override def failed[T](exception: Throwable): Future[T] = Future.failed(exception)

  override def flatMap[T, R](effect: Future[T], function: T => Future[R]): Future[R] = effect.flatMap(function)

  override def either[T](effect: Future[T]): Future[Either[Throwable, T]] = effect.transform(value => Success(value.toEither))
}
