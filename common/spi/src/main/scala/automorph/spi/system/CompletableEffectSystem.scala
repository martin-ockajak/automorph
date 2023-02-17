package automorph.spi.system

import automorph.spi.EffectSystem

/**
 * Computational effect system plugin supporting completable effects.
 *
 * The underlying runtime must support monadic composition of effectful values.
 *
 * @tparam Effect
 *   effect type (similar to IO Monad in Haskell)
 */
trait CompletableEffectSystem[Effect[_]] extends EffectSystem[Effect] {

  /**
   * Creates a completable effect.
   *
   * @tparam T
   *   effectful value type
   * @return
   *   completable effect
   */
  def completable[T]: Effect[Completable[Effect, T]]
}
