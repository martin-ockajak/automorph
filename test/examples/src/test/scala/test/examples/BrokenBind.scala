package test.examples

import automorph.DefaultHandler
import automorph.system.IdentitySystem.Identity
import automorph.spi.EffectSystem

trait BrokenBind[Effect[_]] {

  def system: EffectSystem[Effect]

  private val api: BrokenApi[Effect] = BrokenApiImpl()
//  private val api: BrokenApiImpl[Effect] = BrokenApiImpl()

  private val handler = DefaultHandler[Effect, Unit](system)
    .bind(api)
    .bind(api)
}

trait BrokenApi[Effect[_]] {

  def test(test: String): Effect[String]
}

final case class BrokenApiImpl[Effect[_]]() extends BrokenApi[Effect] {

  def test(test: String): Effect[String] = ???
}
