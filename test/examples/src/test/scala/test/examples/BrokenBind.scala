package test.examples

import automorph.DefaultHandler
import automorph.spi.EffectSystem

trait BrokenBind[Effect[_], Context] {

  def system: EffectSystem[Effect]

  private val brokenApiInstance: BrokenApi[Effect] = BrokenApiImpl()

  private val handler = DefaultHandler[Effect, Context](system)
    .bind(brokenApiInstance)
    .bind(brokenApiInstance)
}

trait BrokenApi[Effect[_]] {

  def test(test: String): Effect[String]
}

final case class BrokenApiImpl[Effect[_]]() extends BrokenApi[Effect] {

  def test(test: String): Effect[String] = ???
}
