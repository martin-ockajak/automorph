package test.system

import automorph.spi.system.{Defer, Deferred}

/**
 * Effect system with deferred effects test.
 *
 * @tparam Effect effect type
 */
trait DeferEffectSystemTest[Effect[_]] extends EffectSystemTest[Effect] {

  "" - {
    system match {
      case _: Defer[?] =>
        "Deferred" - {
          "Success" in {
            val outcome = system.flatMap(system.asInstanceOf[Defer[Effect]].deferred[String]) { deferred =>
              system.flatMap(deferred.succeed(text))(_ => deferred.effect)
            }
            execute(outcome).should(equal(Right(text)))
          }
          "Failure" in {
            val outcome = system.flatMap(system.asInstanceOf[Defer[Effect]].deferred[String]) { deferred =>
              system.flatMap(deferred.fail(error))(_ => deferred.effect)
            }
            execute(outcome).should(equal(Left(error)))
          }
        }
      case _ => {}
    }
  }
}
