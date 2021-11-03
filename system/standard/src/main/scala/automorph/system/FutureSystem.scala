package automorph.system

import automorph.spi.EffectSystem
import automorph.spi.system.{Defer, Deferred}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

/**
 * Asynchronous effect system plugin using Future as an effect type.
 *
 * @see [[https://docs.scala-lang.org/overviews/core/futures.html Library documentation]]
 * @see [[https://www.scala-lang.org/api/current/scala/concurrent/Future.html Effect type]]
 * @constructor Creates an asynchronous effect system plugin using `Future` as an effect type.
 * @param executionContext execution context
 */
final case class FutureSystem()(
  implicit val executionContext: ExecutionContext
) extends EffectSystem[Future] with Defer[Future] {

  override def wrap[T](value: => T): Future[T] =
    Future(value)

  override def pure[T](value: T): Future[T] =
    Future.successful(value)

  override def failed[T](exception: Throwable): Future[T] =
    Future.failed(exception)

  override def either[T](effect: Future[T]): Future[Either[Throwable, T]] =
    effect.transform(value => Success(value.toEither))

  override def flatMap[T, R](effect: Future[T], function: T => Future[R]): Future[R] =
    effect.flatMap(function)

  override def run[T](effect: Future[T]): Unit =
    ()

  override def deferred[T]: Future[Deferred[Future, T]] = {
    val promise = Promise[T]()
    Future.successful(Deferred(
      promise.future,
      (result: T) => Future {
        promise.success(result)
        ()
      },
      (error: Throwable) => Future {
        promise.failure(error)
        ()
      }
    ))
  }
}

object FutureSystem {
  /**
   * Effect type.
   *
   * @tparam T value type
   */
  type Effect[T] = Future[T]
}
