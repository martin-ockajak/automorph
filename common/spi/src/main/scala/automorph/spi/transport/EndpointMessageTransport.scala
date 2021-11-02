package automorph.spi.transport

import automorph.spi.MessageTransport

/**
 * Endpoint message transport protocol plugin.
 *
 * Used to passively handle requests into responses using specific message transport protocol
 * from an active server while invoking RPC request handler to process them.
 */
trait EndpointMessageTransport extends MessageTransport
