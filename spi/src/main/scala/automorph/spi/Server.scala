package automorph.spi

/**
 * RPC server plugin.
 *
 * Used to actively receive and reply to requests using specific message transport protocol
 * while invoking RPC request handler to process them.
 */
trait Server extends AutoCloseable {}
