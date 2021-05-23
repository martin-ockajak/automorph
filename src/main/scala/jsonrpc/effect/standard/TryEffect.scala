package jsonrpc.effect.native

import jsonrpc.spi.Effect
import scala.util.{Failure, Success, Try}

/**
 * Try effect system plugin.
 *
 * @see [[https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html Documentation]]
 * @see Effect type: [[scala.util.Try]]
 */
final case class TryEffect() extends Effect[Try]:

  def pure[T](value: T): Try[T] = Success(value)

  def failed[T](exception: Throwable): Try[T] = Failure(exception)

  def map[T, R](effect: Try[T], function: T => R): Try[R] = effect.map(function)

  def either[T](value: Try[T]): Try[Either[Throwable, T]] = Success(value.toEither)
