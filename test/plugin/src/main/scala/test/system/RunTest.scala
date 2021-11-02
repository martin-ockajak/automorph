package test.system

import automorph.spi.EffectSystem
import automorph.spi.system.Run

/**
 * Effect system with deferred effects test.
 *
 * @tparam Effect effect type
 */
trait RunTest[Effect[_]] extends EffectSystemTest[Effect] {

  "" - {
    system match {
      case _: Run[_] =>
        "Run" in {
          system.asInstanceOf[Run[Effect]].run(system.wrap(text))
        }
      case _ => {}
    }
  }
}
