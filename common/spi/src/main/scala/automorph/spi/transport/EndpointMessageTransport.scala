package automorph.spi.transport

import automorph.spi.MessageTransport

/**
 * Endpoint message transport protocol plugin.
 *
 * Passively parses requests to be processed by the RPC handler and creates responses for specific transport protocol.
 */
trait EndpointMessageTransport extends MessageTransport
