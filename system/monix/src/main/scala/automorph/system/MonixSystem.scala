package automorph.system

import automorph.spi.EffectSystem
import monix.eval.Task

/**
 * Monix effect effect system plugin using `Task` as an effect type.
 *
 * @see [[https://monix.io/ Documentation]]
 * @see [[https://monix.io/api/current/monix/eval/Task.html Effect type]]
 * @constructor Creates a Monix effect system plugin using `Task` as an effect type.
 */
final case class MonixSystem() extends EffectSystem[Task] {

  override def wrap[T](value: => T): Task[T] = Task.evalAsync(value)

  override def pure[T](value: T): Task[T] = Task.pure(value)

  override def failed[T](exception: Throwable): Task[T] = Task.raiseError(exception)

  override def either[T](effect: Task[T]): Task[Either[Throwable, T]] = effect.attempt

  override def flatMap[T, R](effect: Task[T], function: T => Task[R]): Task[R] = effect.flatMap(function)
}

object MonixSystem {
  /**
   * Effect type.
   *
   * @tparam T value type
   */
  type Effect[T] = Task[T]
}
