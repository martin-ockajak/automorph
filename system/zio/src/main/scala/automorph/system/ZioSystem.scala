package automorph.system

import automorph.spi.EffectSystem
import automorph.spi.system.{Defer, Deferred}
import zio.{Queue, RIO, ZQueue}

/**
 * ZIO effect system plugin using `RIO` as an effect type.
 *
 * @see [[https://zio.dev Library documentation]]
 * @see [[https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html Effect type]]
 * @constructor Creates a ZIO effect system plugin using `RIO` as an effect type.
 * @tparam Environment ZIO environment type
 */
final case class ZioSystem[Environment]()
  extends EffectSystem[({ type Effect[T] = RIO[Environment, T] })#Effect]
  with Defer[({ type Effect[T] = RIO[Environment, T] })#Effect] {

  override def wrap[T](value: => T): RIO[Environment, T] =
    RIO(value)

  override def pure[T](value: T): RIO[Environment, T] =
    RIO.succeed(value)

  override def failed[T](exception: Throwable): RIO[Environment, T] =
    RIO.fail(exception)

  override def either[T](effect: RIO[Environment, T]): RIO[Environment, Either[Throwable, T]] =
    effect.either

  override def flatMap[T, R](effect: RIO[Environment, T], function: T => RIO[Environment, R]): RIO[Environment, R] =
    effect.flatMap(function)

  override def deferred[T]: RIO[Environment, Deferred[({ type Effect[T] = RIO[Environment, T] })#Effect, T]] =
    map(
      ZQueue.sliding[Either[Throwable, T]](1),
      (queue: Queue[Either[Throwable, T]]) =>
        Deferred(
          queue.take.flatMap {
            case Right(result) => pure(result)
            case Left(error) => failed(error)
          },
          result => map(queue.offer(Right(result)), _ => ()),
          error => map(queue.offer(Left(error)), _ => ())
        )
    )
}

object ZioSystem {

  /**
   * Creates a ZIO effect system plugin without environment requirements using `Task` as an effect type.
   *
   * @see [[https://zio.dev Library documentation]]
   * @see [[https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html Effect type]]
   * @return ZIO effect system
   */
  def default: ZioSystem[Any] =
    ZioSystem[Any]()
}
