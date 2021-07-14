package automorph.spi

/**
 * Message transport protocol plugin.
 *
 * The underlying transport protocol must support implementation of request-response pattern.
 *
 * @tparam Effect effect type
 * @tparam Context request context type
 */
trait Transport[Effect[_], Context]
