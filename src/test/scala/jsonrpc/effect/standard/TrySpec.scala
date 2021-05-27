package jsonrpc.effect.standard

import jsonrpc.effect.EffectSpec
import jsonrpc.effect.native.TryEffect
import jsonrpc.spi.Effect
import scala.util.{Failure, Success, Try}

class TrySpec extends EffectSpec[Try]:
  def effect: Effect[Try] = TryEffect()

  def run[T](outcome: Try[T]): Either[Throwable, T] = outcome.toEither
