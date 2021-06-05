package jsonrpc.backend.standard

import jsonrpc.spi.Backend
import scala.util.{Failure, Success, Try}

/**
 * Try effect backend plugin.
 *
 * @see [[https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html Documentation]]
 * @see Effect type: [[scala.util.Try]]
 */
final case class TryBackend() extends Backend[Try]:

  def pure[T](value: T): Try[T] = Success(value)

  def failed[T](exception: Throwable): Try[T] = Failure(exception)

  def flatMap[T, R](effect: Try[T], function: T => Try[R]): Try[R] = effect.flatMap(function)

  def either[T](value: Try[T]): Try[Either[Throwable, T]] = Success(value.toEither)
