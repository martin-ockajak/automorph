package jsonrpc.spi

trait EffectContext[Effect[_]]:
  def unit[T](value: T): Effect[T]

  def map[T, R](value: Effect[T], function: T => R): Effect[R]

  def either[T](value: Effect[T]): Effect[Either[Throwable, T]]
