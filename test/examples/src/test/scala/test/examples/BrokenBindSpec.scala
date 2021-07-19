package test.examples

import automorph.Handler
import automorph.format.DefaultUpickleCustom
import automorph.format.json.UpickleJsonFormat
import automorph.spi.EffectSystem
import test.base.BaseSpec

trait BrokenBindSpec extends BaseSpec {

  /** Effect type. */
  type Effect[_]
  /** Request context type. */
  type Context

  def system: EffectSystem[Effect]

  val simpleApiInstance: SimpleApi[Effect] = SimpleApiImpl(system)
  val complexApiInstance: ComplexApi[Effect, Context] = ComplexApiImpl(system)

  def handler: Handler.AnyFormat[Effect, Context] = {
    Handler[UpickleJsonFormat.Node, UpickleJsonFormat[DefaultUpickleCustom.type], Effect, Context](UpickleJsonFormat(), system)
      .bind(simpleApiInstance).bind(complexApiInstance)
  }
}
