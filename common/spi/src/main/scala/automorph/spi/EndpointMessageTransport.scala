package automorph.spi

/**
 * Endpoint message transport protocol plugin.
 *
 * Used to passively receive and reply to requests using specific message transport protocol
 * from an active server while invoking RPC request handler to process them
 */
trait EndpointMessageTransport extends MessageTransport
