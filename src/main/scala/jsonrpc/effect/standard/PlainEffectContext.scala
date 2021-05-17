package jsonrpc.effect.native

import jsonrpc.spi.EffectContext

final case class PlainEffectContext() 
  extends EffectContext[PlainEffectContext.Id]:
  
  def pure[T](value: T): T = value

  def map[T, R](value: T, function: T => R): R = function(value)

  def either[T](value: T): Either[Throwable, T] = Right(value)

object PlainEffectContext:
  type Id[T] = T
