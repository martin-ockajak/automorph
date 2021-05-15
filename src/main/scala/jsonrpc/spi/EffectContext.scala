package jsonrpc.spi

trait EffectContext[Effect[_]]:
  def unit[T](value: T): Effect[T]

  def transform[T](result: Effect[T], success: (T) => Unit, failure: (Throwable) => Unit): Effect[Unit]
