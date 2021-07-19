package test.examples

import automorph.Handler
import automorph.DefaultHandler
import automorph.format.DefaultUpickleCustom
import automorph.format.json.UpickleJsonFormat
import automorph.spi.EffectSystem

trait BrokenBind[Effect[_], Context] {

  def system: EffectSystem[Effect]

  private val brokenApiInstance: BrokenApi[Effect] = BrokenApiImpl(system)

  private val handler = DefaultHandler[Effect, Context](system)
    .bind(brokenApiInstance)
    .bind(EmptyApi())
}

trait BrokenApi[Effect[_]] {

  def test(test: String): Effect[String]
}

final case class BrokenApiImpl[Effect[_]](backend: EffectSystem[Effect]) extends BrokenApi[Effect] {

  override def test(test: String): Effect[String] = backend.pure(test)
}

final case class EmptyApi()
