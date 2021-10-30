package test.system

import automorph.spi.EffectSystem
import automorph.spi.system.{Defer, Deferred}
import scala.util.{Failure, Success, Try}
import test.base.BaseTest

/**
 * Effect system with deferred effects test.
 *
 * @tparam Effect effect type
 */
trait DeferEffectSystemTest[Effect[_]] extends EffectSystemTest[Effect] {

  def deferSystem: EffectSystem[Effect] with Defer[Effect]

  override def system: EffectSystem[Effect] = deferSystem

  "" - {
    "Deferred" - {
      "Success" in {
        val outcome = system.flatMap(
          deferSystem.deferred[String],
          (deferred: Deferred[Effect, String]) => {
            system.flatMap(deferred.succeed(text), _ => deferred.effect)
          }
        )
        run(outcome).should(equal(Right(text)))
      }
      "Failure" in {
        val outcome = system.flatMap(
          deferSystem.deferred[String],
          (deferred: Deferred[Effect, String]) => {
            system.flatMap(deferred.fail(error), _ => deferred.effect)
          }
        )
        run(outcome).should(equal(Left(error)))
      }
    }
  }
}
