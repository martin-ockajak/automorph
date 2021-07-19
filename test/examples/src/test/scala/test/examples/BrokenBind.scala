package test.examples

import automorph.Handler
import automorph.format.DefaultUpickleCustom
import automorph.format.json.UpickleJsonFormat
import automorph.spi.EffectSystem

trait BrokenBind[Effect[_], Context] {

  def system: EffectSystem[Effect]

  private val simpleApiInstance: SimpleApi[Effect] = SimpleApiImpl(system)
  private val complexApiInstance: ComplexApi[Effect, Context] = ComplexApiImpl(system)

  private val handler = Handler[UpickleJsonFormat.Node, UpickleJsonFormat[DefaultUpickleCustom.type], Effect, Context](
    UpickleJsonFormat(),
    system
  )
    .bind(simpleApiInstance)
    .bind(complexApiInstance)
}

trait SimpleApi[Effect[_]] {

  def test(test: String): Effect[String]
}

final case class SimpleApiImpl[Effect[_]](backend: EffectSystem[Effect]) extends SimpleApi[Effect] {

  override def test(test: String): Effect[String] = backend.pure(test)
}

trait ComplexApi[Effect[_], Context] {
}

final case class ComplexApiImpl[Effect[_], Context](backend: EffectSystem[Effect]) extends ComplexApi[Effect, Context] {
}
