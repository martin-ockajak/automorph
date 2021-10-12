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

  /** Default asynchronous effectful computation backend plugin type. */
  type AsyncType = EffectSystem[Future]

  /** Default synchronous effectful computation backend plugin type. */
  type SyncType = EffectSystem[Identity]

  /**
   * Creates a default asynchronous effect ''system'' plugin using `Future` as an effect type.
   *
   * @return asynchronous effect ''system'' plugin
   */
  def async(implicit executionContext: ExecutionContext): AsyncType = FutureSystem()

  /**
   * Creates a default synchronous effect ''system'' plugin using `Identity` as an effect type.
   *
   * @return synchronous effect ''system'' plugin
   */
  def sync: SyncType = IdentitySystem()
}
