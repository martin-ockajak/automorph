package jsonrpc.effect.native

import jsonrpc.spi.EffectContext

final case class PlainEffect()
  extends EffectContext[PlainEffect.Plain]:

  def pure[T](value: T): T = value

  def map[T, R](value: T, function: T => R): R =
    function(value)

  def either[T](value: T): Either[Throwable, T] =
    Right(value)

object PlainEffect:
  type Plain[T] = T
