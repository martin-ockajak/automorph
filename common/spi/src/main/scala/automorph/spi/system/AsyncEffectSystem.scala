package automorph.spi.system

import automorph.spi.EffectSystem

/**
 * Asynchronous computational effect system plugin.
 *
 * The underlying runtime must support monadic composition of effectful values
 * and creation of externally completable effects.
 *
 * @tparam Effect
 *   effect type (similar to IO Monad in Haskell)
 */
trait AsyncEffectSystem[Effect[_]] extends EffectSystem[Effect] {

  /**
   * Creates an externally completable effect.
   *
   * @tparam T
   *   effectful value type
   * @return
   *   completable effect
   */
  def completable[T]: Effect[Completable[Effect, T]]
}
