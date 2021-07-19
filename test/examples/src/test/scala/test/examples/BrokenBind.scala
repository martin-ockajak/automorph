package test.examples

import automorph.DefaultHandler
import automorph.system.IdentitySystem.Identity
import automorph.spi.EffectSystem

trait BrokenBind[Effect[_]] {

  def system: EffectSystem[Effect]

  private val brokenApi: BrokenApi[Effect] = BrokenApiImpl()
//  private val brokenApi: BrokenApiImpl[Effect] = BrokenApiImpl()

  private val handler = DefaultHandler[Effect, Unit](system)
    .bind(brokenApi)
    .bind(brokenApi)
}

trait BrokenApi[Effect[_]] {

  def test(test: String): Effect[String]
}

final case class BrokenApiImpl[Effect[_]]() extends BrokenApi[Effect] {

  def test(test: String): Effect[String] = ???
}
