package jsonrpc.effect.monix

import jsonrpc.effect.EffectSpec
import jsonrpc.spi.Effect
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try

class MonixEffectSpec extends EffectSpec[Task] :
  def effect: Effect[Task] = MonixEffect()

  def run[T](outcome: Task[T]): Either[Throwable, T] = Try(outcome.runSyncUnsafe(Duration.Inf)).toEither
