package automorph.system

import automorph.spi.EffectSystem
import automorph.spi.system.{Defer, Deferred, Run}
import monix.eval.Task
import monix.catnap.MVar
import monix.execution.Scheduler

/**
 * Monix effect effect system plugin using `Task` as an effect type.
 *
 * @see [[https://monix.io/ Library documentation]]
 * @see [[https://monix.io/api/current/monix/eval/Task.html Effect type]]
 * @constructor Creates a Monix effect system plugin using `Task` as an effect type.
 */
case class MonixSystem() extends EffectSystem[Task] with Defer[Task] {

  override def wrap[T](value: => T): Task[T] =
    Task.evalAsync(value)

  override def pure[T](value: T): Task[T] =
    Task.pure(value)

  override def failed[T](exception: Throwable): Task[T] =
    Task.raiseError(exception)

  override def either[T](effect: Task[T]): Task[Either[Throwable, T]] =
    effect.attempt

  override def flatMap[T, R](effect: Task[T], function: T => Task[R]): Task[R] =
    effect.flatMap(function)

  override def deferred[T]: Task[Deferred[Task, T]] =
    map(
      MVar.empty[Task, Either[Throwable, T]](),
      (mVar: MVar[Task, Either[Throwable, T]]) =>
        Deferred(
          mVar.read.flatMap {
            case Right(result) => pure(result)
            case Left(error) => failed(error)
          },
          result =>
            flatMap(
              mVar.tryPut(Right(result)),
              (success: Boolean) =>
                Option.when(success)(pure(())).getOrElse {
                  failed(new IllegalStateException("Deferred effect already resolved"))
                }
            ),
          error =>
            flatMap(
              mVar.tryPut(Left(error)),
              (success: Boolean) =>
                Option.when(success)(pure(())).getOrElse {
                  failed(new IllegalStateException("Deferred effect already resolved"))
                }
            )
        )
    )
}

object MonixSystem {

  /**
   * Effect type.
   *
   * @tparam T value type
   */
  type Effect[T] = Task[T]

  /**
   * Creates Monix effect effect system plugin using `Task` as an effect type.
   *
   * @see [[https://monix.io/ Library documentation]]
   * @see [[https://monix.io/api/current/monix/eval/Task.html Effect type]]
   * @param scheduler task scheduler
   * @return Monix effect system plugin
   */
  def apply(implicit scheduler: Scheduler): MonixSystem with Run[Task] =
    new MonixSystem with Run[Task] {

      override def run[T](effect: Task[T]): Unit =
        effect.runAsyncAndForget
    }
}
