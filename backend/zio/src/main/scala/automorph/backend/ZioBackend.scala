package automorph.backend

import automorph.spi.Backend
import zio.RIO

/**
 * ZIO backend plugin using `RIO` as an effect type.
 *
 * @see [[https://zio.dev/ Documentation]]
 * @see [[https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html Effect type]]
 * @constructor Creates a ZIO backend plugin using `RIO` as an effect type.
 * @tparam Environment ZIO environment type
 */
final case class ZioBackend[Environment]() extends Backend[({ type Effect[T] = RIO[Environment, T] })#Effect] {

  override def pure[T](value: T): RIO[Environment, T] = RIO.succeed(value)

  override def failed[T](exception: Throwable): RIO[Environment, T] = RIO.fail(exception)

  override def flatMap[T, R](value: RIO[Environment, T], function: T => RIO[Environment, R]): RIO[Environment, R] =
    value.flatMap(function)

  override def either[T](value: RIO[Environment, T]): RIO[Environment, Either[Throwable, T]] = value.either
}

case object ZioBackend {
  /**
   * Effect type constructor.
   *
   * @tparam Environment ZIO environment
   */
  type TaskEffect[Environment] = RIO[Any, Environment]
}
