package automorph.system

import automorph.spi.EffectSystem
import monix.eval.Task

/**
 * Monix effect backend plugin using `Task` as an effect type.
 *
 * @see [[https://monix.io/ Documentation]]
 * @see [[https://monix.io/api/current/monix/eval/Task.html Effect type]]
 * @constructor Creates a Monix backend plugin using `Task` as an effect type.
 */
final case class MonixBackend() extends EffectSystem[Task] {

  override def pure[T](value: T): Task[T] = Task.pure(value)

  override def failed[T](exception: Throwable): Task[T] = Task.raiseError(exception)

  override def flatMap[T, R](effect: Task[T], function: T => Task[R]): Task[R] = effect.flatMap(function)

  override def either[T](value: Task[T]): Task[Either[Throwable, T]] = value.attempt
}

case object MonixBackend {
  /**
   * Effect type.
   *
   * @tparam T value type
   */
  type Effect[T] = Task[T]
}
