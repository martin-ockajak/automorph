package test.examples

import automorph.{Default, Handler}
import automorph.spi.EffectSystem
import automorph.system.IdentitySystem.Identity

trait BrokenBind {

  type Effect[T] = Identity[T]
//  type Effect[_]

  def system: EffectSystem[Effect]

  private val api = BrokenApiImpl[Effect]()
//  private val api: BrokenApi[Effect] = BrokenApiImpl()

  Handler.protocol(Default.protocol).system(system).context[Unit]
//    .brokenBind(api)
  Seq(api)
}

trait BrokenApi[Effect[_]] {

  def test(test: String): Effect[Unit]
}

//final case class BrokenApiImpl[Effect[_]]() extends BrokenApi[Effect] {
final case class BrokenApiImpl[Effect[_]]() {

  def test(test: String): Effect[Unit] = ???
}
