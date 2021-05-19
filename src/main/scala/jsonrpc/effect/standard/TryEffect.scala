package jsonrpc.effect.native

import jsonrpc.spi.Effect
import scala.util.{Success, Try}
import jsonrpc.core.ScalaSupport.*

final case class TryEffect()
  extends Effect[Try]:
  
  def pure[T](value: T): Try[T] =
    value.asSuccess

  def map[T, R](effect: Try[T], function: T => R): Try[R] =
    effect.map(function)

  def either[T](value: Try[T]): Try[Either[Throwable, T]] =
    value.toEither.asSuccess
