package jsonrpc.effect.standard

import jsonrpc.effect.EffectSpec
import jsonrpc.effect.standard.NoEffect
import jsonrpc.effect.standard.NoEffect.Identity
import jsonrpc.spi.Effect
import scala.util.{Failure, Success, Try}

class NoSpec extends EffectSpec[Identity] :
  def effect: Effect[Identity] = NoEffect()

  def run[T](outcome: Identity[T]): Either[Throwable, T] = Try(outcome).toEither
