package test.system

import automorph.spi.EffectSystem
import automorph.spi.system.{Defer, Deferred}

/**
 * Effect system with deferred effects test.
 *
 * @tparam Effect effect type
 */
trait DeferTest[Effect[_]] extends EffectSystemTest[Effect] {

  "" - {
    system match {
      case _: Defer[_] =>
        "Deferred" - {
          "Success" in {
            val outcome = system.flatMap(
              system.asInstanceOf[Defer[Effect]].deferred[String],
              (deferred: Deferred[Effect, String]) => {
                system.flatMap(deferred.succeed(text), (_: Unit) => deferred.effect)
              }
            )
            execute(outcome).should(equal(Right(text)))
          }
          "Failure" in {
            val outcome = system.flatMap(
              system.asInstanceOf[Defer[Effect]].deferred[String],
              (deferred: Deferred[Effect, String]) => {
                system.flatMap(deferred.fail(error), (_: Unit) => deferred.effect)
              }
            )
            execute(outcome).should(equal(Left(error)))
          }
        }
      case _ => {}
    }
  }
}
