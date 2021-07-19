package test.examples

import automorph.DefaultHandler
import automorph.spi.EffectSystem
import automorph.system.IdentitySystem.Identity

trait BrokenBind[Effect[_]] {

  def system: EffectSystem[Effect]

  private val api: BrokenApi[Effect] = BrokenApiImpl()
//  private val api: BrokenApiImpl[Effect] = BrokenApiImpl()

  private val handler = DefaultHandler[Effect, Unit](system)
    .bind(api)
    .bind(api)
}

trait BrokenApi[Effect[_]] {

  def test(test: String): Effect[Unit]
}

final case class BrokenApiImpl[Effect[_]]() extends BrokenApi[Effect] {

  def test(test: String): Effect[Unit] = ???
}
