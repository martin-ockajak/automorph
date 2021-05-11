package jsonrpc.spi

trait EffectContext[Effect[_]]:
  def unit[T](value: T): Effect[T]
