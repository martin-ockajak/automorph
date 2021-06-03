package jsonrpc.effect.scalaz

import jsonrpc.effect.EffectSpec
import jsonrpc.spi.Effect
import scalaz.effect.IO
import scala.util.Try

class ScalazEffectSpec extends EffectSpec[IO]:
  def effect: Effect[IO] = ScalazEffect()

  def run[T](outcome: IO[T]): Either[Throwable, T] = Try(outcome.unsafePerformIO()).toEither
