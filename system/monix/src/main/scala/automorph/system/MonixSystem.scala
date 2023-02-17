package automorph.system

import automorph.spi.system.{Completable, CompletableEffectSystem}
import monix.catnap.MVar
import monix.eval.Task
import monix.execution.Scheduler

/**
 * Monix effect effect system plugin using `Task` as an effect type.
 *
 * @see
 *   [[https://monix.io/ Library documentation]]
 * @see
 *   [[https://monix.io/api/current/monix/eval/Task.html Effect type]]
 * @constructor
 *   Creates a Monix effect system plugin using `Task` as an effect type.
 * @param scheduler
 *   task scheduler
 */
final case class MonixSystem()(implicit val scheduler: Scheduler) extends CompletableEffectSystem[Task] {

  override def wrap[T](value: => T): Task[T] =
    Task.evalAsync(value)

  override def pure[T](value: T): Task[T] =
    Task.pure(value)

  override def failed[T](exception: Throwable): Task[T] =
    Task.raiseError(exception)

  override def either[T](effect: => Task[T]): Task[Either[Throwable, T]] =
    effect.attempt

  override def flatMap[T, R](effect: Task[T])(function: T => Task[R]): Task[R] =
    effect.flatMap(function)

  override def run[T](effect: Task[T]): Unit =
    effect.runAsyncAndForget

  override def completable[T]: Task[Completable[Task, T]] =
    map(MVar.empty[Task, Either[Throwable, T]]())(CompletableTask(_))

  private case class CompletableTask[T](private val mVar: MVar[Task, Either[Throwable, T]])
    extends Completable[Task, T]() {

    override def effect: Task[T] =
      mVar.read.flatMap {
        case Right(result) => pure(result)
        case Left(error) => failed(error)
      }

    override def succeed(value: T): Task[Unit] =
      flatMap(mVar.tryPut(Right(value))) { success =>
        Option.when(success)(pure(())).getOrElse {
          failed(new IllegalStateException("Deferred effect already resolved"))
        }
      }

    override def fail(exception: Throwable): Task[Unit] =
      flatMap(mVar.tryPut(Left(exception))) { success =>
        Option.when(success)(pure(())).getOrElse {
          failed(new IllegalStateException("Deferred effect already resolved"))
        }
      }
  }
}

object MonixSystem {

  /**
   * Effect type.
   *
   * @tparam T
   *   value type
   */
  type Effect[T] = Task[T]
}
