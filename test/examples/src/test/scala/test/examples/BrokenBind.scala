package test.examples

import automorph.Handler
import automorph.format.DefaultUpickleCustom
import automorph.format.json.UpickleJsonFormat
import automorph.spi.EffectSystem

trait BrokenBind[Effect[_], Context] {

  def system: EffectSystem[Effect]

  private val brokenApiInstance: BrokenApi[Effect] = BrokenApiImpl(system)
  private val emptyApiInstance: EmptyApi = EmptyApiImpl()

  private val handler = Handler[UpickleJsonFormat.Node, UpickleJsonFormat[DefaultUpickleCustom.type], Effect, Context](
    UpickleJsonFormat(),
    system
  )
    .bind(brokenApiInstance)
    .bind(emptyApiInstance)
}

trait BrokenApi[Effect[_]] {

  def test(test: String): Effect[String]
}

final case class BrokenApiImpl[Effect[_]](backend: EffectSystem[Effect]) extends BrokenApi[Effect] {

  override def test(test: String): Effect[String] = backend.pure(test)
}

trait EmptyApi

final case class EmptyApiImpl() extends EmptyApi
