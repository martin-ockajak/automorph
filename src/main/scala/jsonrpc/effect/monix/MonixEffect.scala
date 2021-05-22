package jsonrpc.effect.monix

import jsonrpc.spi.Effect
import monix.eval.Task

/**
 * Monix effect system plugin.
 *
 * @see [[https://monix.io/ Documentation]]
 * @see [[https://monix.io/api/current/monix/eval/Task.html Effect type]]
 */
final case class MonixEffect[Environment]() extends Effect[Task]:

  def pure[T](value: T): Task[T] = Task.pure(value)

  def map[T, R](effect: Task[T], function: T => R): Task[R] = effect.map(function)

  def either[T](value: Task[T]): Task[Either[Throwable, T]] = value.attempt
