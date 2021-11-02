package automorph.system

import automorph.spi.EffectSystem
import automorph.spi.system.{Defer, Deferred, Run}
import zio.{Queue, RIO, Runtime, Task, ZEnv, ZQueue}

/**
 * ZIO effect system plugin using `RIO` as an effect type.
 *
 * @see [[https://zio.dev Library documentation]]
 * @see [[https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html Effect type]]
 * @constructor Creates a ZIO effect system plugin using `RIO` as an effect type.
 * @param runtime runtime system
 * @tparam Environment ZIO environment type
 */
case class ZioSystem[Environment]()(
  implicit val runtime: Runtime[Environment] = Runtime.default.withReportFailure(_ => ())
) extends EffectSystem[({ type Effect[A] = RIO[Environment, A] })#Effect]
  with Run[({ type Effect[A] = RIO[Environment, A] })#Effect]
  with Defer[({ type Effect[A] = RIO[Environment, A] })#Effect] {

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

  override def run[T](effect: RIO[Environment, T]): Unit =
    runtime.unsafeRunAsync(effect)(_ => ())

  override def deferred[T]: RIO[Environment, Deferred[({ type Effect[A] = RIO[Environment, A] })#Effect, T]] =
    map(
      ZQueue.dropping[Either[Throwable, T]](1),
      (queue: Queue[Either[Throwable, T]]) => {
        Deferred(
          queue.take.flatMap {
            case Right(result) => pure(result)
            case Left(error) => failed(error)
          },
          result => map(queue.offer(Right(result)), (_: Boolean) => ()),
          error => map(queue.offer(Left(error)), (_: Boolean) => ())
        )
      }
    )
}

object ZioSystem {

  /**
   * ZIO with default environment effect type.
   *
   * @tparam T effectful value type
   */
  type DefaultEffect[T] = RIO[ZEnv, T]

  /**
   * Creates a ZIO effect system plugin with default environment using `RIO` as an effect type.
   *
   * @see [[https://zio.dev Library documentation]]
   * @see [[https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html Effect type]]
   * @return ZIO effect system plugin
   */
  def default: ZioSystem[ZEnv] with Run[DefaultEffect] =
    ZioSystem[ZEnv]()
}
