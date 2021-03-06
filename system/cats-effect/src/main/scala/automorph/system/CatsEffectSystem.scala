package automorph.system

import automorph.spi.EffectSystem
import automorph.spi.system.{Defer, Deferred}
import cats.effect.IO
import cats.effect.std.Queue
import cats.effect.unsafe.IORuntime

/**
 * Cats Effect effect system plugin.
 *
 * @see [[https://typelevel.org/cats-effect/ Library documentation]]
 * @see [[https://typelevel.org/cats-effect/api/3.x/cats/effect/IO.html Effect type]]
 * @constructor Creates a Cats Effect effect system plugin using `IO` as an effect type.
 * @param runtime runtime system
 */
final case class CatsEffectSystem()(
  implicit val runtime: IORuntime
) extends EffectSystem[IO] with Defer[IO] {

  override def wrap[T](value: => T): IO[T] =
    IO(value)

  override def pure[T](value: T): IO[T] =
    IO.pure(value)

  override def failed[T](exception: Throwable): IO[T] =
    IO.raiseError(exception)

  override def either[T](effect: => IO[T]): IO[Either[Throwable, T]] =
    effect.attempt

  override def flatMap[T, R](effect: IO[T])(function: T => IO[R]): IO[R] =
    effect.flatMap(function)

  override def run[T](effect: IO[T]): Unit =
    effect.unsafeRunAndForget()

  override def deferred[T]: IO[Deferred[IO, T]] = {
    map(Queue.dropping[IO, Either[Throwable, T]](1)) { queue =>
      Deferred(
        queue.take.flatMap {
          case Right(result) => pure(result)
          case Left(error) => failed(error)
        },
        result =>
          flatMap(queue.tryOffer(Right(result))) { success =>
            Option.when(success)(pure(())).getOrElse {
              failed(new IllegalStateException("Deferred effect already resolved"))
            }
          },
        error =>
          flatMap(queue.tryOffer(Left(error))) { success =>
            Option.when(success)(pure(())).getOrElse {
              failed(new IllegalStateException("Deferred effect already resolved"))
            }
          }
      )
    }
  }
}

object CatsEffectSystem {
  /**
   * Effect type.
   *
   * @tparam T value type
   */
  type Effect[T] = IO[T]
}
