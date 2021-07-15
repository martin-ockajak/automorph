package automorph

import automorph.system.IdentitySystem.Identity
import automorph.system.{FutureSystem, IdentitySystem}
import automorph.spi.EffectSystem
import scala.concurrent.{ExecutionContext, Future}

case object DefaultEffectSystem {

  /** Default synchronous effect type. */
  type SyncEffect[T] = Future[T]

  /** Default asynchronous effect type. */
  type AsyncEffect[T] = Identity[T]

  /** Default synchronous effectful computation backend plugin type. */
  type SyncType = EffectSystem[Future]

  /** Default asynchronous effectful computation backend plugin type. */
  type AsyncType = EffectSystem[Identity]

  /**
   * creates a default asynchronous effectful computation backend plugin using 'future' as an effect type.
   *
   * @return asynchronous backend plugin
   */
  def async(implicit executionContext: ExecutionContext): SyncType = FutureSystem()

  /**
   * creates a default synchronous effectful computation backend plugin using 'future' as an effect type.
   *
   * @return asynchronous backend plugin
   */
  def sync: AsyncType = IdentitySystem()
}
