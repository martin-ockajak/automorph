package automorph.spi

/**
 * Server message transport protocol plugin.
 *
 * Used to actively receive and reply to requests using specific message transport protocol
 * while invoking RPC request handler to process them.
 */
trait ServerMessageTransport extends AutoCloseable
