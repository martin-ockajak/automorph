package test.system

import automorph.spi.system.AsyncEffectSystem

/**
 * Completable effect system test.
 *
 * @tparam Effect
 *   effect type
 */
trait AsyncEffectSystemTest[Effect[_]] extends EffectSystemTest[Effect] {

  "" - {
    system match {
      case _: AsyncEffectSystem[?] =>
        "Completable" - {
          "Success" in {
            val effect = system.flatMap(completableSystem.completable[String]) { completable =>
              system.flatMap(completable.succeed(text))(_ => completable.effect)
            }
            run(effect).should(equal(Right(text)))
          }
          "Failure" in {
            val effect = system.flatMap(completableSystem.completable[String]) { completable =>
              system.flatMap(completable.fail(error))(_ => completable.effect)
            }
            run(effect).should(equal(Left(error)))
          }
        }
      case _ => {}
    }
  }

  private def completableSystem: AsyncEffectSystem[Effect] =
    system.asInstanceOf[AsyncEffectSystem[Effect]]
}
