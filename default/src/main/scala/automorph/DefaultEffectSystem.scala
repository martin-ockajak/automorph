package automorph

import automorph.system.IdentitySystem.Identity
import automorph.system.{FutureSystem, IdentitySystem}
import automorph.spi.EffectSystem
import scala.concurrent.{ExecutionContext, Future}

object DefaultEffectSystem {

  /** Default synchronous effect type. */
  type SyncEffect[T] = Future[T]

  /** Default asynchronous effect type. */
  type AsyncEffect[T] = Identity[T]

  /**
   * Creates a default asynchronous effect ''system'' plugin using `Future` as an effect type.
   *
   * @return asynchronous effect ''system'' plugin
   */
  def async(implicit executionContext: ExecutionContext): EffectSystem[Future] = FutureSystem()

  /**
   * Creates a default synchronous effect ''system'' plugin using `Identity` as an effect type.
   *
   * @return synchronous effect ''system'' plugin
   */
  def sync: EffectSystem[Identity] = IdentitySystem()
}
