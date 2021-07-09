package automorph

import automorph.DefaultTypes.{DefaultAsyncBackend, DefaultSyncBackend}
import automorph.backend.IdentityBackend.Identity
import automorph.backend.{FutureBackend, IdentityBackend}
import scala.concurrent.ExecutionContext

case object DefaultBackend {

  /**
   * creates a default asynchronous effectful computation backend plugin using 'future' as an effect type.
   *
   * @return asynchronous backend plugin
   */
  def async(implicit executionContext: ExecutionContext): DefaultAsyncBackend = FutureBackend()

  /**
   * creates a default synchronous effectful computation backend plugin using 'future' as an effect type.
   *
   * @return asynchronous backend plugin
   */
  def sync: DefaultSyncBackend = IdentityBackend()
}
