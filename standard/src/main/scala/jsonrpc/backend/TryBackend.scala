package jsonrpc.backend

import jsonrpc.spi.Backend
import scala.util.{Failure, Success, Try}

/**
 * Try effect synchronous backend plugin.
 *
 * @see [[https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html Documentation]]
 * @see Effect type: [[scala.util.Try]]
 */
final case class TryBackend() extends Backend[Try] {

  override def pure[T](value: T): Try[T] = Success(value)

  override def failed[T](exception: Throwable): Try[T] = Failure(exception)

  override def flatMap[T, R](effect: Try[T], function: T => Try[R]): Try[R] = effect.flatMap(function)

  override def either[T](value: Try[T]): Try[Either[Throwable, T]] = Success(value.toEither)
}
