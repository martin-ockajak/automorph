package jsonrpc.effect.standard

import jsonrpc.effect.EffectSpec
import jsonrpc.spi.Effect
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import concurrent.ExecutionContext.Implicits.global

class FutureSpec extends EffectSpec[Future]:
  def effect: Effect[Future] = FutureEffect()

  def run[T](outcome: Future[T]): Either[Throwable, T] = Try(outcome.await).toEither
