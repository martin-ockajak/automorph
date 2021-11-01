package test.system

import automorph.spi.EffectSystem
import automorph.spi.system.{Defer, Deferred}

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
            system.flatMap(deferred.succeed(text), (_: Unit) => deferred.effect)
          }
        )
        run(outcome).should(equal(Right(text)))
      }
      "Failure" in {
        val outcome = system.flatMap(
          deferSystem.deferred[String],
          (deferred: Deferred[Effect, String]) => {
            system.flatMap(deferred.fail(error), (_: Unit) => deferred.effect)
          }
        )
        run(outcome).should(equal(Left(error)))
      }
    }
  }
}
