package automorph.spi

/**
 * Server message transport protocol plugin.
 *
 * Actively receives requests to be processed by the RPC handler and sends responses using specific transport protocol.
 *
 * @tparam Effect
 *   effect type
 * @tparam Context
 *   message context type
 */
trait ServerMessageTransport[Effect[_], Context] {

  /**
   * Closes this server transport freeing the underlying resources.
   *
   * @return
   *   nothing
   */
  def close(): Effect[Unit]
}
