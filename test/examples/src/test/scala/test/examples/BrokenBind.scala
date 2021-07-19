package test.examples

import automorph.DefaultHandler
import automorph.system.IdentitySystem.Identity
import automorph.spi.EffectSystem

trait BrokenBind[Effect[_]] {

  def system: EffectSystem[Effect]

  private val brokenApiInstance: BrokenApi[Effect] = BrokenApiImpl()
//  private val brokenApiInstance: BrokenApiImpl[Effect] = BrokenApiImpl()

  private val handler = DefaultHandler[Effect, Unit](system)
    .bind(brokenApiInstance)
    .bind(brokenApiInstance)
}

trait BrokenApi[Effect[_]] {

  def test(test: String): Effect[String]
}

final case class BrokenApiImpl[Effect[_]]() extends BrokenApi[Effect] {

  def test(test: String): Effect[String] = ???
}
