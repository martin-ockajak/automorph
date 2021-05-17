package jsonrpc.effect.monix

import jsonrpc.spi.EffectContext
import monix.eval.Task

final case class MonixEffectContext[Environment]() 
  extends EffectContext[Task]:

  def pure[T](value: T): Task[T] = Task.pure(value)

  def map[T, R](value: Task[T], function: T => R): Task[R] = value.map(function)

  def either[T](value: Task[T]): Task[Either[Throwable, T]] = value.attempt
