package automorph.system

import automorph.spi.EffectSystem
import zio.RIO

/**
 * ZIO backend plugin using `RIO` as an effect type.
 *
 * @see [[https://zio.dev/ Documentation]]
 * @see [[https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html Effect type]]
 * @constructor Creates a ZIO backend plugin using `RIO` as an effect type.
 * @tparam Environment ZIO environment type
 */
final case class ZioSystem[Environment]() extends EffectSystem[({ type Effect[T] = RIO[Environment, T] })#Effect] {

  override def wrap[T](value: => T): RIO[Environment, T] = RIO(value)

  override def pure[T](value: T): RIO[Environment, T] = RIO.succeed(value)

  override def failed[T](exception: Throwable): RIO[Environment, T] = RIO.fail(exception)

  override def either[T](effect: RIO[Environment, T]): RIO[Environment, Either[Throwable, T]] = effect.either

  override def flatMap[T, R](effect: RIO[Environment, T], function: T => RIO[Environment, R]): RIO[Environment, R] =
    effect.flatMap(function)
}

object ZioSystem {
  /**
   * Effect type constructor.
   *
   * @tparam Environment ZIO environment
   */
  type TaskEffect[Environment] = RIO[Any, Environment]
}
