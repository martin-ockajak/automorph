package test.examples

import automorph.Handler
import automorph.format.DefaultUpickleCustom
import automorph.format.json.UpickleJsonFormat
import automorph.spi.EffectSystem
import test.base.BaseSpec

trait BrokenBindSpec[Effect[_], Context] extends BaseSpec {

  def system: EffectSystem[Effect]

  val simpleApiInstance: SimpleApi[Effect] = SimpleApiImpl(system)
  val complexApiInstance: ComplexApi[Effect, Context] = ComplexApiImpl(system)

  private val handler = {
    Handler[UpickleJsonFormat.Node, UpickleJsonFormat[DefaultUpickleCustom.type], Effect, Context](UpickleJsonFormat(), system)
      .bind(simpleApiInstance)
      .bind(complexApiInstance)
  }
}
