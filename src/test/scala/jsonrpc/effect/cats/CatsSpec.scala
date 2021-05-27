package jsonrpc.effect.cats

import jsonrpc.effect.EffectSpec
import jsonrpc.spi.Effect
import scala.util.Try
import cats.effect.IO

class CatsSpec extends EffectSpec[IO] :
  def effect: Effect[IO] = CatsEffect()

  def run[T](outcome: IO[T]): Either[Throwable, T] = Try(outcome.unsafeRunSync()).toEither
