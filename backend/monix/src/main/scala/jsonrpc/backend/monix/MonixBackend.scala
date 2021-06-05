package jsonrpc.backend.monix

import jsonrpc.spi.Backend
import monix.eval.Task

/**
 * Monix effect backend plugin.
 *
 * @see [[https://monix.io/ Documentation]]
 * @see [[https://monix.io/api/current/monix/eval/Task.html Effect type]]
 */
final case class MonixBackend() extends Backend[Task]:

  def pure[T](value: T): Task[T] = Task.pure(value)

  def failed[T](exception: Throwable): Task[T] = Task.raiseError(exception)

  def flatMap[T, R](effect: Task[T], function: T => Task[R]): Task[R] = effect.flatMap(function)

  def either[T](value: Task[T]): Task[Either[Throwable, T]] = value.attempt
