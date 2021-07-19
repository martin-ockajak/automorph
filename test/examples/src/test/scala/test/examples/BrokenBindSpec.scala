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
  type SimpleApiType = SimpleApi[Effect]
  type ComplexApiType = ComplexApi[Effect, Context]

  def system: EffectSystem[Effect]

  val simpleApiInstance: SimpleApiType = SimpleApiImpl(system)
  val complexApiInstance: ComplexApiType = ComplexApiImpl(system)

  def handler: Handler.AnyFormat[Effect, Context] = {
    val format = UpickleJsonFormat()
    Handler[UpickleJsonFormat.Node, UpickleJsonFormat[DefaultUpickleCustom.type], Effect, Context](format, system)
      .bind(simpleApiInstance).bind(complexApiInstance)
  }
}
