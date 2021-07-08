package automorph.spi

/**
 * Endpoint server plugin.
 *
 * The server can be used to receive and reply to requests using specific message transport protocol
 * while invoking JSON-RPC request handler to process them.
 */
trait Server extends AutoCloseable {}
