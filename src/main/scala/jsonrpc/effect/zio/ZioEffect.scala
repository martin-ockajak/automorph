package jsonrpc.effect.zio

import jsonrpc.spi.Effect
import zio.{RIO, Task}

/**
 * ZIO effect system plugin.
 *
 * @see [[https://zio.dev/ Documentation]]
 * @see [[https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html Effect type]]
 */
final case class ZioEffect[Environment]() extends Effect[[T] =>> RIO[Environment, T]]:

  def pure[T](value: T): RIO[Environment, T] = RIO.succeed(value)

  def failed[T](exception: Throwable): RIO[Environment, T] = RIO.fail(exception)

  def flatMap[T, R](value: RIO[Environment, T], function: T => RIO[Environment, R]): RIO[Environment, R] = value.flatMap(function)

  def either[T](value: RIO[Environment, T]): RIO[Environment, Either[Throwable, T]] = value.either
