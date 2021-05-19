package jsonrpc.effect.native

import jsonrpc.spi.EffectContext
import scala.util.{Success, Try}

final case class TryEffect()
  extends EffectContext[Try]:
  
  def pure[T](value: T): Try[T] =
    Success(value)

  def map[T, R](value: Try[T], function: T => R): Try[R] =
    value.map(function)

  def either[T](value: Try[T]): Try[Either[Throwable, T]] =
    Success(value.toEither)
