package automorph

import automorph.system.IdentitySystem.Identity
import automorph.system.{FutureSystem, IdentitySystem}
import automorph.spi.EffectSystem
import scala.concurrent.{ExecutionContext, Future}

object DefaultEffectSystem {

  /** Default asynchronous effect type. */
  type AsyncEffect[T] = Future[T]

  /** Default synchronous effect type. */
  type SyncEffect[T] = Identity[T]

  /** Default asynchronous effect system plugin type. */
  type AsyncType = EffectSystem[Future]

  /** Default synchronous effect system plugin type. */
  type SyncType = EffectSystem[Identity]

  /**
   * Creates a default asynchronous effect system plugin.
   *
   * @see [[https://docs.scala-lang.org/overviews/core/futures.html Documentation]]
   * @see [[https://www.scala-lang.org/api/current/scala/concurrent/Future.html Effect type]]
   * @return asynchronous effect system plugin
   */
  def async(implicit executionContext: ExecutionContext): AsyncType = FutureSystem()

  /**
   * Creates a default synchronous effect system plugin.
   *
   * @see [[https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem$$Identity.html Effect type]]
   * @return synchronous effect system plugin
   */
  def sync: SyncType = IdentitySystem()
}
