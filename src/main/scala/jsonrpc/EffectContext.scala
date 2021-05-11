package jsonrpc

trait EffectContext[Effect[_]]:
  def unit[T](value: T): Effect[T]
