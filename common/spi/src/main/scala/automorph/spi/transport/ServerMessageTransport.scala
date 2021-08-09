package automorph.spi.transport

import automorph.spi.MessageTransport

/**
 * Server message transport protocol plugin.
 *
 * Used to actively receive and reply to requests using specific message transport protocol
 * while invoking RPC request handler to process them.
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
