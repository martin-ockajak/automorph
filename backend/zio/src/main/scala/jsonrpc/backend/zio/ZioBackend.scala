package jsonrpc.backend.zio

import jsonrpc.spi.Backend
import zio.{RIO, Task}

/**
 * ZIO effect backend plugin.
 *
 * @see [[https://zio.dev/ Documentation]]
 * @see [[https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html Effect type]]
 * @tparam Environment ZIO environment type
 */
final case class ZioBackend[Environment]() extends Backend[[T] =>> RIO[Environment, T]]:

  override def pure[T](value: T): RIO[Environment, T] = RIO.succeed(value)

  override def failed[T](exception: Throwable): RIO[Environment, T] = RIO.fail(exception)

  override def flatMap[T, R](value: RIO[Environment, T], function: T => RIO[Environment, R]): RIO[Environment, R] = value.flatMap(function)

  override def either[T](value: RIO[Environment, T]): RIO[Environment, Either[Throwable, T]] = value.either
