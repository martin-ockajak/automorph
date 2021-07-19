package test.examples

import automorph.spi.EffectSystem

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
