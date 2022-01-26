package automorph.spi.transport

import automorph.spi.MessageTransport

/**
 * Server message transport protocol plugin.
 *
 * Actively receives requests to be processed by the RPC handler and sends responses using specific transport protocol.
 *
 * @tparam Effect effect type
 */
trait ServerMessageTransport[Effect[_]] extends MessageTransport {
  /**
   * Closes this server transport freeing the underlying resources.
   *
   * @return nothing
   */
  def close(): Effect[Unit]
}
