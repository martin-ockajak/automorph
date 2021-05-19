package jsonrpc.spi

trait Effect[E[_]]:
  def pure[T](value: T): E[T]

  def map[T, R](value: E[T], function: T => R): E[R]

  def either[T](value: E[T]): E[Either[Throwable, T]]
