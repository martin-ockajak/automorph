package jsonrpc.effect.native

import jsonrpc.spi.Effect
import scala.util.{Success, Try}

/**
 * Try effect system plugin.
 *
 * Documentation: https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html
 * Effect type: Try
 * Effect type API: https://www.scala-lang.org/api/2.13.6/scala/util/Try.html
 */
final case class TryEffect()
  extends Effect[Try]:
  
  def pure[T](value: T): Try[T] =
    Success(value)

  def map[T, R](effect: Try[T], function: T => R): Try[R] =
    effect.map(function)

  def either[T](value: Try[T]): Try[Either[Throwable, T]] =
    Success(value.toEither)
